package org.jellyfin.androidtv.ui.syncplay

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayPlayer
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayPlayerEvent
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayPlayerSnapshot
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.SyncPlayQueueItem
import java.util.UUID
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/** Bridges SyncPlay to the shared Media3 player used by the rewrite video screen. */
class CoreSyncPlayPlayer(
	private val api: ApiClient,
	private val playbackManager: PlaybackManager,
	private val videoQueueManager: VideoQueueManager,
	private val navigationRepository: NavigationRepository,
) : SyncPlayPlayer {
	private val mutableEvents = MutableSharedFlow<SyncPlayPlayerEvent>(extraBufferCapacity = 16)
	override val events = mutableEvents.asSharedFlow()
	private var preparingItem: UUID? = null
	private var ready = CompletableDeferred<Unit>()

	private val backendListener = object : PlayerBackendEventListener() {
		override fun onBuffering() {
			mutableEvents.tryEmit(SyncPlayPlayerEvent.Buffering)
		}

		override fun onReady() {
			if (currentItemId() == preparingItem) ready.complete(Unit)
			mutableEvents.tryEmit(SyncPlayPlayerEvent.Ready)
		}

		override fun onMediaStreamEnd(mediaStream: org.jellyfin.playback.core.mediastream.PlayableMediaStream) {
			mutableEvents.tryEmit(SyncPlayPlayerEvent.Ended)
		}

		override fun onPlayStateChange(state: PlayState) {
			if (state == PlayState.ERROR) {
				ready.completeExceptionally(IllegalStateException("Media3 playback failed"))
				mutableEvents.tryEmit(SyncPlayPlayerEvent.Failed)
			}
		}
	}

	init {
		playbackManager.addBackendEventListener(backendListener)
	}

	override val snapshot: SyncPlayPlayerSnapshot
		get() = SyncPlayPlayerSnapshot(
			positionTicks = playbackManager.state.positionInfo.active.inWholeMilliseconds * TICKS_PER_MILLISECOND,
			isPlaying = playbackManager.state.playState.value == PlayState.PLAYING,
		)

	override suspend fun prepare(item: SyncPlayQueueItem, positionTicks: Long) {
		val baseItem = withContext(Dispatchers.IO) {
			api.userLibraryApi.getItem(itemId = item.itemId).content
		}
		preparingItem = item.itemId
		ready = CompletableDeferred()
		videoQueueManager.setCurrentVideoQueue(listOf(baseItem))
		videoQueueManager.setCurrentMediaPosition(0)
		navigationRepository.navigate(
			Destinations.videoPlayerNew((positionTicks / TICKS_PER_MILLISECOND).toInt()),
			replace = true,
		)
		ready.await()
		playbackManager.state.pause()
		seek(positionTicks)
		preparingItem = null
	}

	override suspend fun seek(positionTicks: Long) {
		val durationMillis = playbackManager.state.positionInfo.duration.inWholeMilliseconds
		val targetMillis = (positionTicks / TICKS_PER_MILLISECOND)
			.coerceAtLeast(0)
			.let { target -> if (durationMillis > 0) target.coerceAtMost(durationMillis) else target }
		playbackManager.state.pause()
		playbackManager.state.seek(targetMillis.milliseconds)
		repeat(SEEK_ATTEMPTS) {
			if (abs(playbackManager.state.positionInfo.active.inWholeMilliseconds - targetMillis) <= SEEK_TOLERANCE_MILLIS) return
			delay(SEEK_POLL_MILLIS)
		}
		error("Media3 seek did not settle")
	}

	override fun pause() = playbackManager.state.pause()
	override fun unpause() = playbackManager.state.unpause()
	override fun stop() = playbackManager.state.stop()

	private fun currentItemId() = playbackManager.queue.entry.value?.baseItem?.id

	private companion object {
		const val TICKS_PER_MILLISECOND = 10_000L
		const val SEEK_ATTEMPTS = 20
		const val SEEK_POLL_MILLIS = 50L
		const val SEEK_TOLERANCE_MILLIS = 250L
	}
}
