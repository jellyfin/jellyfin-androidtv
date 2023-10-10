package org.jellyfin.playback.jellyfin.playsession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.jellyfin.queue.item.BaseItemDtoUserQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.QueueItem
import org.jellyfin.sdk.model.extensions.inWholeTicks
import kotlin.math.roundToInt
import org.jellyfin.sdk.model.api.RepeatMode as SdkRepeatMode

class PlaySessionService(
	private val api: ApiClient,
) : PlayerService() {
	private var reportedStream: PlayableMediaStream? = null

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

	private val PlayableMediaStream.baseItem
		get() = when (val entry = queueEntry) {
			is BaseItemDtoUserQueueEntry -> entry.baseItem
			else -> null
		}

	private val MediaConversionMethod.playMethod
		get() = when (this) {
			MediaConversionMethod.None -> PlayMethod.DIRECT_PLAY
			MediaConversionMethod.Remux -> PlayMethod.DIRECT_STREAM
			MediaConversionMethod.Transcode -> PlayMethod.TRANSCODE
		}

	private val RepeatMode.remoteRepeatMode
		get() = when (this) {
			RepeatMode.NONE -> SdkRepeatMode.REPEAT_NONE
			RepeatMode.REPEAT_ENTRY_ONCE -> SdkRepeatMode.REPEAT_ONE
			RepeatMode.REPEAT_ENTRY_INFINITE -> SdkRepeatMode.REPEAT_ALL
		}

	suspend fun sendUpdateIfActive() {
		reportedStream?.let {
			coroutineScope.launch { sendStreamUpdate(it) }
		}
	}

	private fun onMediaStreamChange(stream: PlayableMediaStream?) {
		reportedStream = stream
		onStart()
	}

	private fun onStart() {
		reportedStream?.let {
			coroutineScope.launch { sendStreamStart(it) }
		}
	}

	private fun onStop() {
		reportedStream?.let {
			coroutineScope.launch { sendStreamStop(it) }
		}
	}

	private fun onPause() {
		reportedStream?.let {
			coroutineScope.launch { sendStreamUpdate(it) }
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

	private suspend fun sendStreamStart(stream: PlayableMediaStream) {
		val item = stream.baseItem ?: return
		api.playStateApi.reportPlaybackStart(PlaybackStartInfo(
			itemId = item.id,
			playSessionId = stream.identifier,
			playlistItemId = item.playlistItemId,
			canSeek = true,
			isMuted = state.volume.muted,
			volumeLevel = (state.volume.volume * 100).roundToInt(),
			isPaused = state.playState.value != PlayState.PLAYING,
			aspectRatio = state.videoSize.value.aspectRatio.toString(),
			positionTicks = withContext(Dispatchers.Main) { state.positionInfo.active.inWholeTicks },
			playMethod = stream.conversionMethod.playMethod,
			repeatMode = state.repeatMode.value.remoteRepeatMode,
			nowPlayingQueue = getQueue(),
		))
	}

	private suspend fun sendStreamUpdate(stream: PlayableMediaStream) {
		val item = stream.baseItem ?: return
		api.playStateApi.reportPlaybackProgress(PlaybackProgressInfo(
			itemId = item.id,
			playSessionId = stream.identifier,
			playlistItemId = item.playlistItemId,
			canSeek = true,
			isMuted = state.volume.muted,
			volumeLevel = (state.volume.volume * 100).roundToInt(),
			isPaused = state.playState.value != PlayState.PLAYING,
			aspectRatio = state.videoSize.value.aspectRatio.toString(),
			positionTicks = withContext(Dispatchers.Main) { state.positionInfo.active.inWholeTicks },
			playMethod = stream.conversionMethod.playMethod,
			repeatMode = state.repeatMode.value.remoteRepeatMode,
			nowPlayingQueue = getQueue(),
		))
	}

	private suspend fun sendStreamStop(stream: PlayableMediaStream) {
		val item = stream.baseItem ?: return
		api.playStateApi.reportPlaybackStopped(PlaybackStopInfo(
			itemId = item.id,
			playSessionId = stream.identifier,
			playlistItemId = item.playlistItemId,
			positionTicks = withContext(Dispatchers.Main) { state.positionInfo.active.inWholeTicks },
			failed = false,
			nowPlayingQueue = getQueue(),
		))
	}
}
