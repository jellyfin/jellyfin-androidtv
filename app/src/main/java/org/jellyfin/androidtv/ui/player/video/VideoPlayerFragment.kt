package org.jellyfin.androidtv.ui.player.video

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.base.BaseScreen
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.ui.playback.rewrite.RewriteMediaManager
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.MediaType
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
	private val syncPlay get() = playbackManager.getService<SyncPlayService>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// The group owns its queue and start position, including preparation while paused.
		if (syncPlay?.group?.value != null) return

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
		if (syncPlay?.group?.value == null) playbackManager.state.pause()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		BaseScreen {
			VideoPlayerScreen()
		}
	}

	override fun onPause() {
		super.onPause()

		if (syncPlay?.group?.value == null) playbackManager.state.pause()
	}

	override fun onResume() {
		super.onResume()

		if (syncPlay?.group?.value == null) playbackManager.state.unpause()
	}

	override fun onStop() {
		super.onStop()

		val service = syncPlay
		if (service?.active == true) {
			// Switching to a group audio item moves to Now Playing without ending the group.
			val entry = playbackManager.queue.entry.value
			if (service.isGroupEntry(entry) && entry?.baseItem?.mediaType == MediaType.AUDIO) return
			if (activity?.isChangingConfigurations == true) return
			lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
				// Reset locally before the leave request; lifecycle changes must not pause or stop the group.
				withContext(NonCancellable) { service.endSession() }
			}
			return
		}

		playbackManager.state.stop()
	}
}
