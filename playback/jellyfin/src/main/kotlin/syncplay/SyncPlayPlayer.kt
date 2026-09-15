package org.jellyfin.playback.jellyfin.syncplay

import kotlinx.coroutines.flow.Flow
import org.jellyfin.sdk.model.api.SyncPlayQueueItem

/** Actual local player state. Positions are absolute media ticks, not transcode stream offsets. */
data class SyncPlayPlayerSnapshot(val positionTicks: Long, val isPlaying: Boolean)

sealed interface SyncPlayPlayerEvent {
	data object Buffering : SyncPlayPlayerEvent
	data object Ready : SyncPlayPlayerEvent
	data object Ended : SyncPlayPlayerEvent
	data object Failed : SyncPlayPlayerEvent
}

/**
 * Raw player operations, confined to the coordinator dispatcher (Main on Android). Prepare and seek
 * complete only when the requested media is ready and paused. They must support coroutine cancellation
 * and must never route operations back through the group request gateway.
 */
interface SyncPlayPlayer {
	val events: Flow<SyncPlayPlayerEvent>
	val snapshot: SyncPlayPlayerSnapshot
	suspend fun prepare(item: SyncPlayQueueItem, positionTicks: Long)
	suspend fun seek(positionTicks: Long)
	fun pause()
	fun unpause()
	fun stop()
}
