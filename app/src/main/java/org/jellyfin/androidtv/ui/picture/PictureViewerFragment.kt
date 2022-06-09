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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.FragmentPictureViewerBinding
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.util.createKeyHandler
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PictureViewerFragment : Fragment(), View.OnKeyListener {
	companion object {
		/**
		 * The download API is used by jellyfin-web when loading images. Unfortunately with our
		 * current code it doesn't work for the app. Larger files (gifs, large photos etc.) can
		 * cause the app to go out of memory. This is mostly catched by Glide but it ends up
		 * never displaying the picture in those cases.
		 *
		 * This toggle is left in the code in case we migrate to a different image processor that
		 * potentially fixes the issue.
		 */
		const val USE_DOWNLOAD_API = false
	}

	private val pictureViewerViewModel by sharedViewModel<PictureViewerViewModel>()
	private val api by inject<ApiClient>()
	private lateinit var binding: FragmentPictureViewerBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = FragmentPictureViewerBinding.inflate(inflater, container, false)
		binding.actionPrevious.setOnClickListener { pictureViewerViewModel.showPrevious() }
		binding.actionNext.setOnClickListener { pictureViewerViewModel.showNext() }
		binding.actionPlayPause.setOnClickListener { pictureViewerViewModel.togglePresentation() }
		binding.root.setOnClickListener { toggleActions() }
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		lifecycleScope.launch {
			pictureViewerViewModel.currentItem.filterNotNull().collect { item ->
				binding.itemSwitcher.getNextView<AsyncImageView>().load(item)
				binding.itemSwitcher.showNextView()
			}
		}

		lifecycleScope.launch {
			pictureViewerViewModel.presentationActive.collect { active ->
				binding.actionPlayPause.isActivated = active
			}
		}
	}

	private val keyHandler = createKeyHandler {
		keyDown(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE)
			.body { pictureViewerViewModel.togglePresentation() }

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

		keyDown(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER)
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

	private fun AsyncImageView.load(item: BaseItemDto) {
		val url = when (USE_DOWNLOAD_API) {
			true -> api.libraryApi.getDownloadUrl(
				itemId = item.id,
				includeCredentials = true, // TODO send authentication via header
			)
			false -> api.imageApi.getItemImageUrl(
				itemId = item.id,
				imageType = ImageType.PRIMARY,
				tag = item.imageTags?.get(ImageType.PRIMARY),
				// Ask the server to downscale the image to avoid the app going out of memory
				// unfortunately this can be a bit slow for larger files
				maxWidth = context.resources.displayMetrics.widthPixels,
				maxHeight = context.resources.displayMetrics.heightPixels,
			)
		}

		load(
			url = url,
			blurHash = item.imageBlurHashes?.get(ImageType.PRIMARY)?.get(item.imageTags?.get(ImageType.PRIMARY)),
			aspectRatio = item.primaryImageAspectRatio ?: 1.0,
		)
	}
}
