package org.jellyfin.playback.jellyfin.playsession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.backend.PlayerBackendEvent
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
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
import kotlin.time.Duration
import org.jellyfin.sdk.model.api.RepeatMode as SdkRepeatMode

class PlaySessionService(
	private val api: ApiClient,
) : PlayerService() {
	private var lastReportedStream: PlayableMediaStream? = null
	private var lastReportedPlayState: PlayState? = null

	override suspend fun onInitialize() {
		state.backendEvents
			.onEach { event ->
				when (event) {
					is PlayerBackendEvent.PlayStateChanged -> {
						val stream = event.stream ?: return@onEach
						val position = event.position ?: return@onEach

						when (event.state) {
							PlayState.PLAYING -> onPlayStatePlaying(stream, position)
							PlayState.PAUSED -> onPlayStatePaused(stream, position)
							PlayState.STOPPED, PlayState.ERROR -> sendStreamStop(stream, position)
						}
					}

					is PlayerBackendEvent.MediaStreamEnded -> sendStreamStop(event.stream, event.position)
					is PlayerBackendEvent.MediaStreamReplaced -> sendStreamStop(event.stream, event.position)
					is PlayerBackendEvent.VideoSizeChanged -> Unit
				}
			}
			.launchIn(coroutineScope)
	}

	private suspend fun onPlayStatePlaying(stream: PlayableMediaStream, position: Duration) {
		if (lastReportedStream === stream && lastReportedPlayState == PlayState.PAUSED) {
			lastReportedPlayState = PlayState.PLAYING
			sendStreamUpdate(stream, position, paused = false)
		} else {
			sendStreamStart(stream, position)
		}
	}

	private suspend fun onPlayStatePaused(stream: PlayableMediaStream, position: Duration) {
		if (lastReportedStream !== stream || lastReportedPlayState != PlayState.PLAYING) return
		lastReportedPlayState = PlayState.PAUSED
		sendStreamUpdate(stream, position, paused = true)
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
		coroutineScope.launch {
			manager.queue.entry.value?.mediaStream?.let { stream ->
				sendStreamUpdate(
					stream = stream,
					position = withContext(Dispatchers.Main) { state.positionInfo.active },
					paused = state.playState.value != PlayState.PLAYING
				)
			}
		}
	}

	private suspend fun getQueue(): List<QueueItem> {
		// The queues are lazy loaded so we only load a small amount of items to set as queue on the
		// backend.
		return manager.queue
			.peekNext(15)
			.mapNotNull { it.baseItem }
			.map { QueueItem(id = it.id, playlistItemId = it.playlistItemId) }
	}

	private suspend fun sendStreamStart(stream: PlayableMediaStream, position: Duration) {
		if (lastReportedStream === stream && lastReportedPlayState == PlayState.PLAYING) return
		lastReportedStream = stream
		lastReportedPlayState = PlayState.PLAYING

		val item = stream.queueEntry.baseItem ?: return

		Timber.d("Reporting playback start for ${item.id} at $position")
		runCatching {
			api.playStateApi.reportPlaybackStart(
				PlaybackStartInfo(
					itemId = item.id,
					playSessionId = stream.identifier,
					playlistItemId = item.playlistItemId,
					canSeek = true,
					isMuted = state.volume.muted,
					volumeLevel = (state.volume.volume * 100).roundToInt(),
					isPaused = false,
					aspectRatio = state.videoSize.value.aspectRatio.toString(),
					positionTicks = position.inWholeTicks,
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

	private suspend fun sendStreamUpdate(stream: PlayableMediaStream, position: Duration, paused: Boolean) {
		val item = stream.queueEntry.baseItem ?: return

		Timber.d("Reporting playback progress for ${item.id} at $position")
		runCatching {
			api.playStateApi.reportPlaybackProgress(
				PlaybackProgressInfo(
					itemId = item.id,
					playSessionId = stream.identifier,
					playlistItemId = item.playlistItemId,
					canSeek = true,
					isMuted = state.volume.muted,
					volumeLevel = (state.volume.volume * 100).roundToInt(),
					isPaused = paused,
					aspectRatio = state.videoSize.value.aspectRatio.toString(),
					positionTicks = position.inWholeTicks,
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
		}.onFailure { error -> Timber.w(error, "Failed to send playback update event") }
	}

	private suspend fun sendStreamStop(stream: PlayableMediaStream, position: Duration) {
		if (lastReportedStream === stream && lastReportedPlayState == PlayState.STOPPED) return
		lastReportedStream = stream
		lastReportedPlayState = PlayState.STOPPED

		val item = stream.queueEntry.baseItem ?: return

		Timber.d("Reporting playback stop for ${item.id} at $position")
		runCatching {
			api.playStateApi.reportPlaybackStopped(
				PlaybackStopInfo(
					itemId = item.id,
					playSessionId = stream.identifier,
					playlistItemId = item.playlistItemId,
					positionTicks = position.inWholeTicks,
					failed = false,
					nowPlayingQueue = getQueue(),
				)
			)
		}.onFailure { error -> Timber.w(error, "Failed to send playback stop event") }
	}
}
