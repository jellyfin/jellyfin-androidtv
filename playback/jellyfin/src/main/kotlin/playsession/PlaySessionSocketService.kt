package org.jellyfin.playback.jellyfin.playsession

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.sdk.api.sockets.SocketInstance
import org.jellyfin.sdk.api.sockets.addGeneralCommandsListener
import org.jellyfin.sdk.api.sockets.addPlayStateCommandsListener
import org.jellyfin.sdk.api.sockets.listener.SocketListener
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.PlaystateCommand
import org.jellyfin.sdk.model.extensions.get
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration

class PlaySessionSocketService(
	private val socketInstance: SocketInstance,
	private val playSessionService: PlaySessionService,
) : PlayerService() {
	private var listeners = mutableListOf<SocketListener>()

	override suspend fun onInitialize() {
		// Player control
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
				coroutineScope.launch { playSessionService.sendUpdateIfActive() }
			}
		}

		// Volume control
		listeners += socketInstance.addGeneralCommandsListener(setOf(GeneralCommandType.VOLUME_UP)) {
			state.volume.increaseVolume()
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}

		listeners += socketInstance.addGeneralCommandsListener(setOf(GeneralCommandType.VOLUME_DOWN)) {
			state.volume.decreaseVolume()
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}

		listeners += socketInstance.addGeneralCommandsListener(setOf(GeneralCommandType.SET_VOLUME)) { message ->
			@Suppress("MagicNumber")
			val volume = message["volume"]?.toFloatOrNull()?.div(100f)
			if (volume != null && volume in 0f..1f) state.volume.setVolume(volume)
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}

		listeners += socketInstance.addGeneralCommandsListener(setOf(GeneralCommandType.MUTE)) {
			state.volume.mute()
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}

		listeners += socketInstance.addGeneralCommandsListener(setOf(GeneralCommandType.UNMUTE)) {
			state.volume.unmute()
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}

		listeners += socketInstance.addGeneralCommandsListener(setOf(GeneralCommandType.TOGGLE_MUTE)) {
			when (state.volume.muted) {
				true -> state.volume.unmute()
				false -> state.volume.mute()
			}
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
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
