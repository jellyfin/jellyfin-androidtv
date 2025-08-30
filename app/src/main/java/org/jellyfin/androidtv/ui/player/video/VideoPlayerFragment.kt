package org.jellyfin.androidtv.ui.player.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.ui.playback.rewrite.RewriteMediaManager
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class VideoPlayerFragment : Fragment() {
	companion object {
		const val EXTRA_POSITION: String = "position"
	}

	private val videoQueueManager by inject<VideoQueueManager>()
	private val playbackManager by inject<PlaybackManager>()
	private val api by inject<ApiClient>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Create a queue from the items added to the legacy video queue
		val queueSupplier = RewriteMediaManager.BaseItemQueueSupplier(api, videoQueueManager.getCurrentVideoQueue(), false)
		Timber.i("Created a queue with ${queueSupplier.items.size} items")
		playbackManager.queue.clear()
		playbackManager.queue.addSupplier(queueSupplier)

		// Set position
		arguments?.getInt(EXTRA_POSITION)?.milliseconds?.let {
			lifecycleScope.launch {
				playbackManager.state.seek(it)
			}
		}

		// Pause player until the initial resume
		playbackManager.state.pause()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		JellyfinTheme {
			VideoPlayerScreen()
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
