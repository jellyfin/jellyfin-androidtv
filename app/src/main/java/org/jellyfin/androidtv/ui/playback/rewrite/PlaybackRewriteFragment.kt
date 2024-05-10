package org.jellyfin.androidtv.ui.playback.rewrite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.ui.PlayerSurfaceView
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.android.ext.android.inject
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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Create a queue from the items added to the legacy video queue
		val queue = RewriteMediaManager.BaseItemQueue(api)
		queue.items.addAll(videoQueueManager.getCurrentVideoQueue())
		Timber.i("Created a queue with ${queue.items.size} items")
		playbackManager.state.queue.replaceQueue(queue)

		// Set position
		val position = arguments?.getInt(EXTRA_POSITION) ?: 0
		if (position != 0) {
			lifecycleScope.launch {
				Timber.i("Skipping to queue item $position")
				playbackManager.state.queue.setIndex(position, false)
			}
		}

		// Pause player until the initial resume
		playbackManager.state.pause()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val view = PlayerSurfaceView(requireContext())
		view.playbackManager = playbackManager
		return view
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
