package org.jellyfin.playback.media3.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.PlayerMessage
import org.jellyfin.playback.core.timedevent.TimedEvent
import timber.log.Timber

class ExoPlayerMessageHandler : PlayerMessage.Target {
	companion object {
		const val TYPE_TIMED_EVENT = 1
	}

	@OptIn(UnstableApi::class)
	override fun handleMessage(messageType: Int, message: Any?): Unit = when (messageType) {
		TYPE_TIMED_EVENT -> {
			val timedEvent = message as? TimedEvent ?: return
			handleTimedEventMessage(timedEvent)
		}

		else -> {
			Timber.w("Unknown message type $messageType")
		}
	}

	private fun handleTimedEventMessage(timedEvent: TimedEvent) = when (timedEvent) {
		is TimedEvent.Callback -> timedEvent.callback()
	}
}
