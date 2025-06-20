package org.jellyfin.androidtv.ui.picture

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.FragmentPictureViewerBinding
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.ui.ScreensaverViewModel
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.createKeyHandler
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class PictureViewerFragment : Fragment(), View.OnKeyListener {
	companion object {
		const val ARGUMENT_ITEM_ID = "item_id"
		const val ARGUMENT_ALBUM_SORT_BY = "album_sort_by"
		const val ARGUMENT_ALBUM_SORT_ORDER = "album_sort_order"
		const val ARGUMENT_AUTO_PLAY = "auto_play"
		private val AUTO_HIDE_ACTIONS_DURATION = 4.seconds
	}

	private val screensaverViewModel by activityViewModel<ScreensaverViewModel>()
	private val pictureViewerViewModel by viewModel<PictureViewerViewModel>()
	private val api by inject<ApiClient>()
	private var _binding: FragmentPictureViewerBinding? = null
	private val binding get() = _binding!!

	private var actionHideTimer: Job? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Load requested item in viewmodel
		lifecycleScope.launch {
			val itemId = requireNotNull(arguments?.getString(ARGUMENT_ITEM_ID)?.toUUIDOrNull())
			val albumSortBy = arguments?.getString(ARGUMENT_ALBUM_SORT_BY)?.let {
				ItemSortBy.fromNameOrNull(it)
			} ?: ItemSortBy.SORT_NAME
			val albumSortOrder = arguments?.getString(ARGUMENT_ALBUM_SORT_ORDER)?.let {
				SortOrder.fromNameOrNull(it)
			} ?: SortOrder.ASCENDING
			pictureViewerViewModel.loadItem(itemId, setOf(albumSortBy), albumSortOrder)

			val autoPlay = arguments?.getBoolean(ARGUMENT_AUTO_PLAY) == true
			if (autoPlay) pictureViewerViewModel.startPresentation()
		}

		// Add a screensaver lock when the slide show is active
		var lock: (() -> Unit)? = null
		pictureViewerViewModel.presentationActive.onEach { active ->
			Timber.i("presentationActive=$active")
			lock?.invoke()

			if (active) lock = screensaverViewModel.addLifecycleLock(lifecycle)
		}.launchIn(lifecycleScope)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		_binding = FragmentPictureViewerBinding.inflate(inflater, container, false)
		binding.actionPrevious.setOnClickListener {
			pictureViewerViewModel.showPrevious()
			resetHideActionsTimer()
		}
		binding.actionNext.setOnClickListener {
			pictureViewerViewModel.showNext()
			resetHideActionsTimer()
		}
		binding.actionPlayPause.setOnClickListener {
			pictureViewerViewModel.togglePresentation()
			resetHideActionsTimer()
		}
		binding.root.setOnClickListener { toggleActions() }
		binding.actionPlayPause.requestFocus()
		arrayOf(binding.actionPrevious, binding.actionPlayPause, binding.actionNext).forEach {
			it.setOnFocusChangeListener { _, _ ->
				resetHideActionsTimer()
			}
		}
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		pictureViewerViewModel.currentItem.filterNotNull().onEach { item ->
			binding.itemSwitcher.getNextView<AsyncImageView>().load(item)
			binding.itemSwitcher.showNextView()
		}.launchIn(lifecycleScope)

		pictureViewerViewModel.presentationActive.onEach { active ->
			binding.actionPlayPause.isActivated = active
		}.launchIn(lifecycleScope)
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	private val keyHandler = createKeyHandler {
		keyDown(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE)
			.body {
				pictureViewerViewModel.togglePresentation()
				resetHideActionsTimer()
			}

		keyDown(KeyEvent.KEYCODE_DPAD_LEFT)
			.condition { !binding.actions.isVisible }
			.body { pictureViewerViewModel.showPrevious() }

		keyDown(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
			.body { pictureViewerViewModel.showPrevious() }

		keyDown(KeyEvent.KEYCODE_DPAD_RIGHT)
			.condition { !binding.actions.isVisible }
			.body { pictureViewerViewModel.showNext() }

		keyDown(KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD, KeyEvent.KEYCODE_MEDIA_NEXT)
			.body { pictureViewerViewModel.showNext() }

		keyDown(
			KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
			KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER,
		)
			.condition { !binding.actions.isVisible }
			.body { showActions() }

		keyDown(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BACK)
			.condition { binding.actions.isVisible }
			.body { hideActions() }
	}

	override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean = keyHandler.onKey(event)

	private var focusedActionView: View? = null
	fun showActions(): Boolean {
		if (binding.actions.isVisible) return false

		binding.actions.isVisible = true
		if (focusedActionView?.requestFocus() != true) binding.actionPlayPause.requestFocus()
		binding.actions.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
		return true
	}

	fun hideActions(): Boolean {
		if (!binding.actions.isVisible) return false
		binding.actions.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out).apply {
			setAnimationListener(object : Animation.AnimationListener {
				override fun onAnimationStart(animation: Animation?) = Unit

				override fun onAnimationEnd(animation: Animation?) {
					focusedActionView = binding.actions.findFocus()
					binding.actions.isGone = true
				}

				override fun onAnimationRepeat(animation: Animation?) = Unit
			})
		})
		return true
	}

	fun toggleActions(): Boolean {
		return if (binding.actions.isVisible) hideActions()
		else showActions()
	}

	private fun resetHideActionsTimer() {
		// Cancel existing timer (if it exists)
		actionHideTimer?.cancel()

		// Don't start the timer when already invisible
		if (!binding.actions.isVisible) return

		// Create new timer
		actionHideTimer = lifecycleScope.launch {
			delay(AUTO_HIDE_ACTIONS_DURATION)

			// Only auto-hide when there is an active presentation
			if (pictureViewerViewModel.presentationActive.value) hideActions()
		}
	}

	private fun AsyncImageView.load(item: BaseItemDto) {
		val image = item.itemImages[ImageType.PRIMARY]

		load(
			url = image?.getUrl(
				api = api,
				// Ask the server to downscale the image to avoid the app going out of memory
				// unfortunately this can be a bit slow for larger files
				maxWidth = resources.displayMetrics.widthPixels,
				maxHeight = resources.displayMetrics.heightPixels,
			),
			blurHash = image?.blurHash,
			aspectRatio = image?.aspectRatio?.toDouble() ?: 1.0,
		)
	}
}
