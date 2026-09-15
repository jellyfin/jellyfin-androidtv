package org.jellyfin.playback.jellyfin.syncplay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Raw local operation: applying this action must never send a group control request. */
data class SyncPlayAction(val command: SendCommandType, val positionTicks: Long?)

/**
 * Schedules one authoritative command. Call only after membership, occurrence and readiness checks.
 * Scope and callers must share the player's dispatcher. Cancel before detaching or resetting state.
 */
class SyncPlayCommandScheduler(
	private val scope: CoroutineScope,
	private val serverTime: () -> Instant?,
	private val apply: (SyncPlayAction) -> Unit,
) {
	private var job: Job? = null
	private var scheduled: SendCommand? = null

	fun schedule(command: SendCommand): Boolean {
		if (serverTime() == null) {
			cancel()
			return false
		}
		if (command.command != SendCommandType.STOP && (command.positionTicks?.let { it >= 0 } != true)) return false
		if (scheduled == command) return true
		cancel()
		scheduled = command
		val whenInstant = command.`when`.toSyncPlayInstant()
		job = scope.launch {
			val executionTime = awaitDeadline(whenInstant) ?: return@launch
			val position = command.positionTicks?.let { ticks ->
				if (command.command == SendCommandType.UNPAUSE) {
					val elapsedMillis = Duration.between(whenInstant, executionTime).toMillis().coerceAtLeast(0)
					val elapsedTicks = elapsedMillis.coerceAtMost(Long.MAX_VALUE / TICKS_PER_MILLISECOND) * TICKS_PER_MILLISECOND
					ticks + elapsedTicks.coerceAtMost(Long.MAX_VALUE - ticks)
				} else ticks
			}
			apply(SyncPlayAction(command.command, position))
		}
		return true
	}

	private suspend fun awaitDeadline(deadline: Instant): Instant? {
		var now = serverTime() ?: return null
		while (now < deadline) {
			// Delay is monotonic; recheck in case a new clock estimate moved the deadline.
			delay(Duration.between(now, deadline).toMillis().coerceAtLeast(1))
			now = serverTime() ?: return null
		}
		return now
	}

	fun cancel() {
		job?.cancel()
		job = null
		scheduled = null
	}

	private companion object {
		const val TICKS_PER_MILLISECOND = 10_000L
	}
}

// The SDK's DateTimeSerializer converts incoming UTC/offset dates to the system timezone.
internal fun LocalDateTime.toSyncPlayInstant(): Instant = atZone(ZoneId.systemDefault()).toInstant()
