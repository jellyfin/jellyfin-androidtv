package org.jellyfin.playback.jellyfin.playsession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jellyfin.playback.core.mediastream.MediaConversionMethod
import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.mediastream.mediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.QueueItem
import org.jellyfin.sdk.model.extensions.inWholeTicks
import timber.log.Timber
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.jellyfin.sdk.model.api.PlaybackOrder as SdkPlaybackOrder
import org.jellyfin.sdk.model.api.RepeatMode as SdkRepeatMode

class PlaySessionService(
	private val api: ApiClient,
) : PlayerService() {
	/**
	 * The item the server currently considers playing for this session, captured when the start
	 * report is sent. All reports use this snapshot instead of the live queue entry, because the
	 * queue can already have moved on by the time a request is sent.
	 */
	private class ActiveSession(
		val entry: QueueEntry,
		val item: BaseItemDto,
		val stream: PlayableMediaStream,
	) {
		var position: Duration = Duration.ZERO
	}

	private val reportMutex = Mutex()
	private var activeSession: ActiveSession? = null

	override suspend fun onInitialize() {
		state.playState.onEach { playState ->
			when (playState) {
				PlayState.PLAYING -> startSessionIfNeeded()
				PlayState.PAUSED -> sendStreamUpdate()
				PlayState.STOPPED -> stopActiveSession()
				PlayState.ERROR -> stopActiveSession(failed = true)
			}
		}.launchIn(coroutineScope)

		// ExoPlayer pauses at the end of a media item instead of stopping, so no PlayState.STOPPED
		// is emitted when the queue moves to the next entry. Report the stop for the item that
		// actually played whenever the active entry is replaced (automatic advance, next/previous
		// or a removed entry).
		manager.queue.entry.onEach { entry ->
			val session = activeSession ?: return@onEach
			if (session.entry == entry) return@onEach

			stopSession(session)

			// A manual next/previous can keep the player in the playing state, in which case no
			// PlayState.PLAYING event follows, so start the new entry here.
			if (entry != null && state.playState.value == PlayState.PLAYING) startSession(entry)
		}.launchIn(coroutineScope)

		// Keep a recent position for the active session so the stop report uses the position of
		// the item that played instead of the position of the entry that replaced it.
		coroutineScope.launch {
			while (true) {
				val session = activeSession
				if (session != null && manager.queue.entry.value == session.entry) {
					session.position = currentPosition()
				}

				delay(1.seconds)
			}
		}
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
		if (activeSession == null) return
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

	private suspend fun currentPosition(): Duration = withContext(Dispatchers.Main) {
		state.positionInfo.active
	}

	private suspend fun startSessionIfNeeded() {
		val entry = manager.queue.entry.value ?: return
		if (activeSession?.entry == entry) return

		startSession(entry)
	}

	private suspend fun startSession(entry: QueueEntry) {
		reportMutex.withLock {
			val stream = entry.mediaStream ?: return@withLock
			val item = entry.baseItem ?: return@withLock
			val position = currentPosition()

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
						positionTicks = position.inWholeTicks,
						playMethod = stream.conversionMethod.playMethod,
						repeatMode = state.repeatMode.value.remoteRepeatMode,
						nowPlayingQueue = getQueue(),
						playbackOrder = state.playbackOrder.value.remotePlaybackOrder,
					)
				)

				activeSession = ActiveSession(entry, item, stream).apply { this.position = position }
			}.onFailure { error -> Timber.w(error, "Failed to send playback start event") }
		}
	}

	private suspend fun sendStreamUpdate() {
		reportMutex.withLock {
			val session = activeSession ?: return@withLock
			val position = currentPosition()
			session.position = position

			runCatching {
				api.playStateApi.reportPlaybackProgress(
					PlaybackProgressInfo(
						itemId = session.item.id,
						playSessionId = session.stream.identifier,
						playlistItemId = session.item.playlistItemId,
						canSeek = true,
						isMuted = state.volume.muted,
						volumeLevel = (state.volume.volume * 100).roundToInt(),
						isPaused = state.playState.value != PlayState.PLAYING,
						aspectRatio = state.videoSize.value.aspectRatio.toString(),
						positionTicks = position.inWholeTicks,
						playMethod = session.stream.conversionMethod.playMethod,
						repeatMode = state.repeatMode.value.remoteRepeatMode,
						nowPlayingQueue = getQueue(),
						playbackOrder = state.playbackOrder.value.remotePlaybackOrder,
					)
				)
			}.onFailure { error -> Timber.w(error, "Failed to send playback update event") }
		}
	}

	private suspend fun stopActiveSession(failed: Boolean = false) {
		reportMutex.withLock {
			val session = activeSession ?: return@withLock

			sendStreamStop(session, failed)
		}
	}

	private suspend fun stopSession(session: ActiveSession, failed: Boolean = false) {
		reportMutex.withLock {
			if (activeSession !== session) return@withLock

			sendStreamStop(session, failed)
		}
	}

	private suspend fun sendStreamStop(session: ActiveSession, failed: Boolean) {
		activeSession = null

		runCatching {
			api.playStateApi.reportPlaybackStopped(
				PlaybackStopInfo(
					itemId = session.item.id,
					playSessionId = session.stream.identifier,
					playlistItemId = session.item.playlistItemId,
					positionTicks = session.position.inWholeTicks,
					failed = failed,
					nowPlayingQueue = getQueue(),
				)
			)
		}.onFailure { error -> Timber.w(error, "Failed to send playback stop event") }
	}

	private val PlaybackOrder.remotePlaybackOrder
		get() = when (this) {
			PlaybackOrder.DEFAULT -> SdkPlaybackOrder.DEFAULT
			PlaybackOrder.RANDOM -> SdkPlaybackOrder.SHUFFLE
			PlaybackOrder.SHUFFLE -> SdkPlaybackOrder.SHUFFLE
		}
}
