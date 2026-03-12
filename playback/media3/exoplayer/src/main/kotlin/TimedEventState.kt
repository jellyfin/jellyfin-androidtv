package org.jellyfin.playback.media3.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import org.jellyfin.playback.core.timedevent.BlockActivation
import org.jellyfin.playback.core.timedevent.TimedEvent
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class TimedEventState {
	private companion object {
		const val MESSAGE_TYPE_ACTIVATE = 1
		const val MESSAGE_TYPE_DEACTIVATE = 2
	}

	private class MessagePayload(
		val timedEvent: TimedEvent,
		val position: Duration,
	)

	private val _currentEvents = mutableListOf<Pair<TimedEvent, List<PlayerMessage>>>()

	private val messageHandler = object : PlayerMessage.Target {
		override fun handleMessage(messageType: Int, message: Any?) {
			val payload = message as? MessagePayload ?: return

			when (messageType) {
				MESSAGE_TYPE_ACTIVATE -> onActivate(payload.timedEvent, BlockActivation.Natural(payload.position))
				MESSAGE_TYPE_DEACTIVATE -> onDeactivate(payload.timedEvent, BlockActivation.Natural(payload.position))
			}
		}
	}

	fun setTimedEvents(exoPlayer: ExoPlayer, timedEvents: List<TimedEvent>) {
		val mediaDuration = exoPlayer.duration.takeUnless { it == C.TIME_UNSET }?.milliseconds

		// Removed events that no longer exist
		val currentEventsIterator = _currentEvents.iterator()
		while (currentEventsIterator.hasNext()) {
			val (timedEvent, messages) = currentEventsIterator.next()
			if (timedEvents.any { it == timedEvent }) continue

			Timber.d("Removing timed event ${timedEvent.key}")
			messages.forEach(PlayerMessage::cancel)
			currentEventsIterator.remove()
		}

		for (timedEvent in timedEvents) {
			if (_currentEvents.any { it.first == timedEvent }) continue

			Timber.d("Creating timed event ${timedEvent.key}")
			_currentEvents.add(timedEvent to timedEvent.createMessages(exoPlayer, mediaDuration))
		}
	}

	fun onSeek(from: Duration, to: Duration, duration: Duration) {
		for ((timedEvent) in _currentEvents) {
			if (timedEvent !is TimedEvent.Block) continue
			val eventStart = timedEvent.start.withMediaDuration(duration) ?: continue
			val eventEnd = timedEvent.end.withMediaDuration(duration) ?: continue

			val wasInBlock = from in eventStart..eventEnd
			val isInBlock = to in eventStart..eventEnd

			if (wasInBlock && !isInBlock) onDeactivate(timedEvent, BlockActivation.Seek(from, to))
			if (!wasInBlock && isInBlock) onActivate(timedEvent, BlockActivation.Seek(from, to))
		}
	}

	fun onDurationChange(exoPlayer: ExoPlayer, duration: Duration?) {
		for (index in _currentEvents.indices) {
			val (timedEvent, messages) = _currentEvents[index]

			// Determine if we need to recreate this timed event
			val recreate = when (timedEvent) {
				is TimedEvent.Instant -> timedEvent.position.isNegative()
				is TimedEvent.Block -> timedEvent.start.isNegative() || timedEvent.end.isNegative()
			}
			if (!recreate) continue

			Timber.d("Recreating timed event ${timedEvent.key} because duration changed to $duration")

			// Remove existing messages
			messages.forEach(PlayerMessage::cancel)

			// Add new messages
			_currentEvents[index] = timedEvent to timedEvent.createMessages(exoPlayer, duration)
		}
	}

	private fun TimedEvent.createMessages(exoPlayer: ExoPlayer, mediaDuration: Duration?) = when (this) {
		is TimedEvent.Instant -> {
			val position = position.withMediaDuration(mediaDuration)
			if (position == null) emptyList()
			else listOf(createMessage(exoPlayer, MESSAGE_TYPE_ACTIVATE, position, this))
		}

		is TimedEvent.Block -> {
			val start = start.withMediaDuration(mediaDuration)
			val end = end.withMediaDuration(mediaDuration)
			if (start == null || end == null) emptyList()
			else listOf(
				createMessage(exoPlayer, MESSAGE_TYPE_ACTIVATE, start, this),
				createMessage(exoPlayer, MESSAGE_TYPE_DEACTIVATE, end, this),
			)
		}
	}

	private fun createMessage(
		exoPlayer: ExoPlayer,
		type: Int,
		position: Duration,
		timedEvent: TimedEvent
	) = exoPlayer.createMessage(messageHandler).apply {
		setType(type)
		// Messages at position 0 will never be invoked by ExoPlayer
		setPosition(position.inWholeMilliseconds.coerceAtLeast(1))
		setPayload(
			MessagePayload(
				timedEvent = timedEvent,
				position = position,
			)
		)
		setDeleteAfterDelivery(false)
	}.send()

	private fun onActivate(timedEvent: TimedEvent, metadata: BlockActivation) {
		Timber.d("Activating ${timedEvent.key} with $metadata")
		when (timedEvent) {
			is TimedEvent.Block -> timedEvent.onActivate?.invoke(metadata)
			is TimedEvent.Instant -> timedEvent.onActivate()
		}
	}

	private fun onDeactivate(timedEvent: TimedEvent, metadata: BlockActivation) {
		Timber.d("Deactivating ${timedEvent.key} with $metadata")
		when (timedEvent) {
			is TimedEvent.Block -> timedEvent.onDeactivate?.invoke(metadata)
			is TimedEvent.Instant -> Unit
		}
	}

	private fun Duration.withMediaDuration(duration: Duration?): Duration? = when {
		isNegative() -> when (duration) {
			null -> null
			else -> (duration + this).takeIf { it >= Duration.ZERO }
		}

		else -> this
	}
}
