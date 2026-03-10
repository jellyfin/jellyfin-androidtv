package org.jellyfin.androidtv.syncplay

import org.jellyfin.sdk.model.UUID

class SyncPlayLoopGuard(
	private val nowMs: () -> Long,
	private val queueGuardWindowMs: Long = 1500L,
	private val readyGuardWindowMs: Long = 1000L,
) {
	data class QueueStateKey(
		val playlistItemId: UUID,
		val index: Int,
		val startPositionTicks: Long,
		val isPlaying: Boolean,
	)

	data class ReadyStateKey(
		val playlistItemId: UUID,
		val positionTicks: Long,
		val isPlaying: Boolean,
	)

	private val lock = Any()
	private var lastQueueStateKey: QueueStateKey? = null
	private var lastQueueStateAtMs: Long = 0L
	private var lastReadyStateKey: ReadyStateKey? = null
	private var lastReadyStateAtMs: Long = 0L

	fun shouldProcessQueueState(queueStateKey: QueueStateKey): Boolean {
		val now = nowMs()
		synchronized(lock) {
			if (lastQueueStateKey == queueStateKey && now - lastQueueStateAtMs < queueGuardWindowMs) {
				return false
			}
			lastQueueStateKey = queueStateKey
			lastQueueStateAtMs = now
			return true
		}
	}

	fun shouldSendReady(readyStateKey: ReadyStateKey): Boolean {
		val now = nowMs()
		synchronized(lock) {
			if (lastReadyStateKey == readyStateKey && now - lastReadyStateAtMs < readyGuardWindowMs) {
				return false
			}
			lastReadyStateKey = readyStateKey
			lastReadyStateAtMs = now
			return true
		}
	}
}
