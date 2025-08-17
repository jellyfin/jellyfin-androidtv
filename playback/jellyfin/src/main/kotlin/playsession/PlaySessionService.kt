package org.jellyfin.playback.jellyfin.playsession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackOrder
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.QueueItem
import org.jellyfin.sdk.model.extensions.inWholeTicks
import timber.log.Timber
import kotlin.math.roundToInt
import org.jellyfin.sdk.model.api.RepeatMode as SdkRepeatMode

class PlaySessionService(
	private val api: ApiClient,
) : PlayerService() {
	override suspend fun onInitialize() {
		state.playState.onEach { playState ->
			when (playState) {
				PlayState.PLAYING -> sendStreamStart()
				PlayState.STOPPED -> sendStreamStop()
				PlayState.PAUSED -> sendStreamUpdate()
				PlayState.ERROR -> sendStreamStop()
			}
		}.launchIn(coroutineScope)
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
		coroutineScope.launch { sendStreamUpdate() }
	}

	private suspend fun getQueue(): List<QueueItem> {
		// The queues are lazy loaded so we only load a small amount of items to set as queue on the
		// backend.
		return manager.queue
			.peekNext(15)
			.mapNotNull { it.baseItem }
			.map { QueueItem(id = it.id, playlistItemId = it.playlistItemId) }
	}

	private suspend fun sendStreamStart() {
		val entry = manager.queue.entry.value ?: return
		val stream = entry.mediaStream ?: return
		val item = entry.baseItem ?: return

		runCatching {
			api.playStateApi.reportPlaybackStart(
				PlaybackStartInfo(
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
					playbackOrder = when (state.playbackOrder.value) {
						org.jellyfin.playback.core.model.PlaybackOrder.DEFAULT -> PlaybackOrder.DEFAULT
						org.jellyfin.playback.core.model.PlaybackOrder.RANDOM -> PlaybackOrder.SHUFFLE
						org.jellyfin.playback.core.model.PlaybackOrder.SHUFFLE -> PlaybackOrder.SHUFFLE
					}
				)
			)
		}.onFailure { error -> Timber.w(error, "Failed to send playback start event") }
	}

	private suspend fun sendStreamUpdate() {
		val entry = manager.queue.entry.value ?: return
		val stream = entry.mediaStream ?: return
		val item = entry.baseItem ?: return

		runCatching {
			api.playStateApi.reportPlaybackProgress(
				PlaybackProgressInfo(
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
					playbackOrder = when (state.playbackOrder.value) {
						org.jellyfin.playback.core.model.PlaybackOrder.DEFAULT -> PlaybackOrder.DEFAULT
						org.jellyfin.playback.core.model.PlaybackOrder.RANDOM -> PlaybackOrder.SHUFFLE
						org.jellyfin.playback.core.model.PlaybackOrder.SHUFFLE -> PlaybackOrder.SHUFFLE
					}
				)
			)
		}.onFailure { error -> Timber.w("Failed to send playback update event", error) }
	}

	private suspend fun sendStreamStop() {
		val entry = manager.queue.entry.value ?: return
		val stream = entry.mediaStream ?: return
		val item = entry.baseItem ?: return

		runCatching {
			api.playStateApi.reportPlaybackStopped(
				PlaybackStopInfo(
					itemId = item.id,
					playSessionId = stream.identifier,
					playlistItemId = item.playlistItemId,
					positionTicks = withContext(Dispatchers.Main) { state.positionInfo.active.inWholeTicks },
					failed = false,
					nowPlayingQueue = getQueue(),
				)
			)
		}.onFailure { error -> Timber.w("Failed to send playback stop event", error) }
	}
}
