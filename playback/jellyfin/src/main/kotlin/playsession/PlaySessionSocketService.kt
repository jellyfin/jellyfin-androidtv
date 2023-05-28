package org.jellyfin.playback.jellyfin.playsession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.sdk.api.sockets.SocketInstance
import org.jellyfin.sdk.api.sockets.addPlayStateCommandsListener
import org.jellyfin.sdk.api.sockets.listener.SocketListener
import org.jellyfin.sdk.model.api.PlaystateCommand
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration

class PlaySessionSocketService(
	private val socketInstance: SocketInstance,
) : PlayerService() {
	private var listeners = mutableListOf<SocketListener>()

	override suspend fun onInitialize() {
		listeners += socketInstance.addPlayStateCommandsListener { message ->
			coroutineScope.launch(Dispatchers.Main) {
				when (message.request.command) {
					PlaystateCommand.STOP -> state.stop()
					PlaystateCommand.PAUSE -> state.pause()
					PlaystateCommand.UNPAUSE -> state.unpause()
					PlaystateCommand.NEXT_TRACK -> state.queue.next()
					PlaystateCommand.PREVIOUS_TRACK -> state.queue.previous()
					PlaystateCommand.SEEK -> {
						val to = message.request.seekPositionTicks?.ticks ?: Duration.ZERO
						state.seek(to)
					}
					PlaystateCommand.REWIND -> state.rewind()
					PlaystateCommand.FAST_FORWARD -> state.fastForward()
					PlaystateCommand.PLAY_PAUSE -> when (state.playState.value) {
						PlayState.PLAYING -> state.pause()
						else -> state.unpause()
					}
				}
			}
		}

		coroutineScope.launch {
			try {
				awaitCancellation()
			} finally {
				listeners.removeAll { listener ->
					listener.stop()
					true
				}
			}
		}
	}
}
