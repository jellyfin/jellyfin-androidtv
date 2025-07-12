package org.jellyfin.androidtv.ui.playback.rewrite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.ui.PlayerSubtitleView
import org.jellyfin.playback.core.ui.PlayerSurfaceView
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

/**
 * Temporary fragment used for testing the playback rewrite. This will eventually be replaced with a
 * proper player user interface.
 */
class PlaybackRewriteFragment : Fragment() {
	companion object {
		const val EXTRA_POSITION: String = "position"
	}

	private val videoQueueManager by inject<VideoQueueManager>()
	private val playbackManager by inject<PlaybackManager>()
	private val api by inject<ApiClient>()

	private val interactionTrackerViewModel by activityViewModel<InteractionTrackerViewModel>()
	private var screensaverLock: (() -> Unit)? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Create a queue from the items added to the legacy video queue
		val queueSupplier = RewriteMediaManager.BaseItemQueueSupplier(api, videoQueueManager.getCurrentVideoQueue())
		Timber.i("Created a queue with ${queueSupplier.items.size} items")
		playbackManager.queue.clear()
		playbackManager.queue.addSupplier(queueSupplier)

		// Set position
		val position = arguments?.getInt(EXTRA_POSITION) ?: 0
		if (position != 0) {
			lifecycleScope.launch {
				Timber.i("Skipping to queue item $position")
				playbackManager.queue.setIndex(position, false)
			}
		}

		// Pause player until the initial resume
		playbackManager.state.pause()

		// Add lifecycle listeners
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.RESUMED) {
				playbackManager.state.playState.onEach { playState ->
					if (playState == PlayState.PLAYING && screensaverLock == null) {
						screensaverLock = interactionTrackerViewModel.addLifecycleLock(lifecycle)
					} else if (playState != PlayState.PLAYING && screensaverLock != null) {
						screensaverLock?.invoke()
						screensaverLock = null
					}
				}.launchIn(this)
			}
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return FrameLayout(requireContext()).apply {
			addView(PlayerSurfaceView(requireContext()).also { view ->
				view.playbackManager = playbackManager
			})

			addView(PlayerSubtitleView(requireContext()).also { view ->
				view.playbackManager = playbackManager
			})
		}
	}

	override fun onPause() {
		super.onPause()

		playbackManager.state.pause()
	}

	override fun onResume() {
		super.onResume()

		playbackManager.state.unpause()
	}

	override fun onStop() {
		super.onStop()

		playbackManager.state.stop()
	}
}
