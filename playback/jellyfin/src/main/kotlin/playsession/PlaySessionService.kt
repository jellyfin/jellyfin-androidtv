package org.jellyfin.playback.jellyfin.playsession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.mediastream.MediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.QueueItem
import org.jellyfin.sdk.model.api.RepeatMode
import org.jellyfin.sdk.model.extensions.inWholeTicks
import kotlin.math.roundToInt

class PlaySessionService(
	private val api: ApiClient,
) : PlayerService() {
	private var playSessionId: String? = null
	private var reportedItem: BaseItemDto? = null

	override suspend fun onInitialize() {
		state.streams.current.onEach { stream -> onMediaStreamChange(stream) }.launchIn(coroutineScope)

		state.playState.onEach { playState ->
			when (playState) {
				PlayState.PLAYING -> onStart()
				PlayState.STOPPED -> onStop()
				PlayState.PAUSED -> onPause()
				PlayState.ERROR -> onStop()
			}
		}.launchIn(coroutineScope)
	}

	suspend fun sendUpdateIfActive() {
		reportedItem?.let {
			updateBaseItem(it)
		}
	}

	private fun onMediaStreamChange(stream: MediaStream?) {
		playSessionId = stream?.identifier
		reportedItem = when (val entry = stream?.queueEntry) {
			is BaseItemDtoUserQueueEntry -> entry.baseItem
			else -> null
		}

		onStart()
	}

	private fun onStart() {
		coroutineScope.launch {
			if (reportedItem != null) startBaseItem(reportedItem!!)
		}
	}

	private fun onStop() {
		coroutineScope.launch {
			if (reportedItem != null) {
				stopBaseItem(reportedItem!!)
			}
		}
	}

	private fun onPause() {
		coroutineScope.launch {
			if (reportedItem != null) updateBaseItem(reportedItem!!)
		}
	}

	private suspend fun getQueue(): List<QueueItem> {
		// The queues are lazy loaded so we only load a small amount of items to set as queue on the
		// backend.
		return state.queue
			.peekNext(15)
			.filterIsInstance<BaseItemDtoUserQueueEntry>()
			.map { QueueItem(id = it.baseItem.id, playlistItemId = it.baseItem.playlistItemId) }
	}

	private suspend fun startBaseItem(item: BaseItemDto) {
		api.playStateApi.reportPlaybackStart(PlaybackStartInfo(
			itemId = item.id,
			playSessionId = playSessionId,
			playlistItemId = item.playlistItemId,
			canSeek = true,
			isMuted = state.volume.muted,
			volumeLevel = (state.volume.volume * 100).roundToInt(),
			isPaused = state.playState.value != PlayState.PLAYING,
			aspectRatio = state.videoSize.value.aspectRatio.toString(),
			positionTicks = withContext(Dispatchers.Main) { state.positionInfo.active.inWholeTicks },
			playMethod = PlayMethod.DIRECT_PLAY,
			repeatMode = RepeatMode.REPEAT_NONE,
			nowPlayingQueue = getQueue(),
		))
	}

	private suspend fun updateBaseItem(item: BaseItemDto) {
		api.playStateApi.reportPlaybackProgress(PlaybackProgressInfo(
			itemId = item.id,
			playSessionId = playSessionId,
			playlistItemId = item.playlistItemId,
			canSeek = true,
			isMuted = state.volume.muted,
			volumeLevel = (state.volume.volume * 100).roundToInt(),
			isPaused = state.playState.value != PlayState.PLAYING,
			aspectRatio = state.videoSize.value.aspectRatio.toString(),
			positionTicks = withContext(Dispatchers.Main) { state.positionInfo.active.inWholeTicks },
			playMethod = PlayMethod.DIRECT_PLAY,
			repeatMode = RepeatMode.REPEAT_NONE,
			nowPlayingQueue = getQueue(),
		))
	}

	private suspend fun stopBaseItem(item: BaseItemDto) {
		api.playStateApi.reportPlaybackStopped(PlaybackStopInfo(
			itemId = item.id,
			playSessionId = playSessionId,
			playlistItemId = item.playlistItemId,
			positionTicks = withContext(Dispatchers.Main) { state.positionInfo.active.inWholeTicks },
			failed = false,
			nowPlayingQueue = getQueue(),
		))
	}
}
