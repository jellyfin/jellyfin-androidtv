package org.jellyfin.playback.jellyfin.playsession

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.api.sockets.subscribeGeneralCommand
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.PlaystateCommand
import org.jellyfin.sdk.model.api.PlaystateMessage
import org.jellyfin.sdk.model.extensions.get
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration

class PlaySessionSocketService(
	private val api: ApiClient,
	private val playSessionService: PlaySessionService,
	private val lifecycle: Lifecycle?,
) : PlayerService() {
	override suspend fun onInitialize() {
		coroutineScope.launch(Dispatchers.IO) {
			if (lifecycle == null) subscribe(this)
			else lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { subscribe(this) }
		}
	}

	private fun subscribe(coroutineScope: CoroutineScope) {
		// Player control
		api.webSocket.subscribe<PlaystateMessage>().onEach { message ->
			coroutineScope.launch(Dispatchers.Main) {
				when (message.data?.command) {
					PlaystateCommand.STOP -> state.stop()
					PlaystateCommand.PAUSE -> state.pause()
					PlaystateCommand.UNPAUSE -> state.unpause()
					PlaystateCommand.NEXT_TRACK -> manager.queue.next()
					PlaystateCommand.PREVIOUS_TRACK -> manager.queue.previous()
					PlaystateCommand.SEEK -> {
						val to = message.data?.seekPositionTicks?.ticks ?: Duration.ZERO
						state.seek(to)
					}

					PlaystateCommand.REWIND -> state.rewind()
					PlaystateCommand.FAST_FORWARD -> state.fastForward()
					PlaystateCommand.PLAY_PAUSE -> when (state.playState.value) {
						PlayState.PLAYING -> state.pause()
						else -> state.unpause()
					}

					// Do nothing
					null -> Unit
				}
				coroutineScope.launch { playSessionService.sendUpdateIfActive() }
			}
		}.launchIn(coroutineScope)

		// Volume control
		api.webSocket.subscribeGeneralCommand(GeneralCommandType.VOLUME_UP).onEach {
			state.volume.increaseVolume()
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}.launchIn(coroutineScope)

		api.webSocket.subscribeGeneralCommand(GeneralCommandType.VOLUME_DOWN).onEach {
			state.volume.decreaseVolume()
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}.launchIn(coroutineScope)

		api.webSocket.subscribeGeneralCommand(GeneralCommandType.SET_VOLUME).onEach { message ->
			@Suppress("MagicNumber")
			val volume = message["volume"]?.toFloatOrNull()?.div(100f)
			if (volume != null && volume in 0f..1f) state.volume.setVolume(volume)
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}.launchIn(coroutineScope)

		api.webSocket.subscribeGeneralCommand(GeneralCommandType.MUTE).onEach {
			state.volume.mute()
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}.launchIn(coroutineScope)

		api.webSocket.subscribeGeneralCommand(GeneralCommandType.UNMUTE).onEach {
			state.volume.unmute()
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}.launchIn(coroutineScope)

		api.webSocket.subscribeGeneralCommand(GeneralCommandType.TOGGLE_MUTE).onEach {
			when (state.volume.muted) {
				true -> state.volume.unmute()
				false -> state.volume.mute()
			}
			coroutineScope.launch { playSessionService.sendUpdateIfActive() }
		}.launchIn(coroutineScope)
	}
}
