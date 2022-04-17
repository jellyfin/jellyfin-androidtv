package org.jellyfin.androidtv.data.eventhandling

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper
import org.jellyfin.apiclient.model.entities.MediaType
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.sockets.SocketInstance
import org.jellyfin.sdk.api.sockets.SocketInstanceState
import org.jellyfin.sdk.api.sockets.addGeneralCommandsListener
import org.jellyfin.sdk.api.sockets.addListener
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.LibraryUpdateInfo
import org.jellyfin.sdk.model.api.PlaystateCommand
import org.jellyfin.sdk.model.extensions.getValue
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.jellyfin.sdk.model.socket.LibraryChangedMessage
import org.jellyfin.sdk.model.socket.PlayMessage
import org.jellyfin.sdk.model.socket.PlayStateMessage
import timber.log.Timber
import java.util.UUID

class SocketHandler(
	private val context: Context,
	private val api: ApiClient,
	private val dataRefreshService: DataRefreshService,
	private val mediaManager: MediaManager,
	private val playbackControllerContainer: PlaybackControllerContainer,
) {
	private val coroutineScope = CoroutineScope(Dispatchers.IO)
	private var socketInstance: SocketInstance = createInstance()
	val state = socketInstance.state

	suspend fun updateSession() {
		try {
			api.sessionApi.postCapabilities(
				playableMediaTypes = listOf(MediaType.Video, MediaType.Audio),
				supportsMediaControl = true,
				supportedCommands = listOf(
					GeneralCommandType.DISPLAY_MESSAGE,
					GeneralCommandType.SEND_STRING,
				),
			)
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to update capabilities")
			return
		}

		val isOffline = state.value == SocketInstanceState.DISCONNECTED || state.value == SocketInstanceState.ERROR
		if (isOffline) socketInstance.reconnect()
		else socketInstance.updateCredentials()
	}

	private fun createInstance() = api.ws().apply {
		// Library
		addListener<LibraryChangedMessage> { message -> onLibraryChanged(message.info) }

		// Media playback
		addListener<PlayMessage> { message -> onPlayMessage(message) }
		addListener<PlayStateMessage> { message -> onPlayStateMessage(message) }

		// General commands
		addGeneralCommandsListener(setOf(GeneralCommandType.DISPLAY_CONTENT)) { message ->
			val itemId by message
			val itemUuid = itemId?.toUUIDOrNull()

			if (itemUuid != null) onDisplayContent(itemUuid)
		}
		addGeneralCommandsListener(setOf(GeneralCommandType.DISPLAY_MESSAGE, GeneralCommandType.SEND_STRING)) { message ->
			val header by message
			val text by message
			val string by message

			onDisplayMessage(header, text ?: string)
		}
	}

	private fun onLibraryChanged(info: LibraryUpdateInfo) {
		Timber.d(buildString {
			appendLine("Library changed.")
			appendLine("Added ${info.itemsAdded.size} items")
			appendLine("Removed ${info.itemsRemoved.size} items")
			appendLine("Updated ${info.itemsUpdated.size} items")
		})

		if (info.itemsAdded.any() || info.itemsRemoved.any())
			dataRefreshService.lastLibraryChange = System.currentTimeMillis()
	}

	private fun onPlayMessage(message: PlayMessage) {
		val itemId = message.request.itemIds?.firstOrNull() ?: return

		PlaybackHelper.retrieveAndPlay(
			itemId.toString(),
			false,
			message.request.startPositionTicks,
			context
		)
	}

	@Suppress("ComplexMethod")
	private fun onPlayStateMessage(message: PlayStateMessage) = coroutineScope.launch(Dispatchers.Main) {
		Timber.i("Received PlayStateMessage with command ${message.request.command}")
		val playbackController = playbackControllerContainer.playbackController
		// Audio playback uses the mediaManager, video playback and live tv use the playbackController
		if (mediaManager.isAudioPlayerInitialized) when (message.request.command) {
			PlaystateCommand.STOP -> mediaManager.stopAudio(true)
			PlaystateCommand.PAUSE, PlaystateCommand.UNPAUSE, PlaystateCommand.PLAY_PAUSE -> mediaManager.playPauseAudio()
			PlaystateCommand.NEXT_TRACK -> mediaManager.nextAudioItem()
			PlaystateCommand.PREVIOUS_TRACK -> mediaManager.prevAudioItem()
			// Not implemented
			PlaystateCommand.SEEK,
			PlaystateCommand.REWIND,
			PlaystateCommand.FAST_FORWARD -> Unit
		} else when (message.request.command) {
			PlaystateCommand.STOP -> playbackController?.endPlayback(true)
			PlaystateCommand.PAUSE, PlaystateCommand.UNPAUSE, PlaystateCommand.PLAY_PAUSE -> playbackController?.playPause()
			PlaystateCommand.NEXT_TRACK -> playbackController?.next()
			PlaystateCommand.PREVIOUS_TRACK -> playbackController?.prev()
			PlaystateCommand.SEEK -> playbackController?.seek(
				(message.request.seekPositionTicks ?: 0) / TICKS_TO_MS
			)
			// FIXME get rewind/forward amount from displayprefs
			PlaystateCommand.REWIND -> playbackController?.skip(REWIND_MS)
			PlaystateCommand.FAST_FORWARD -> playbackController?.skip(FORWARD_MS)
		}
	}

	// FIXME: Add an ItemLauncher function that accepts SDK BaseItemDto
	// Add "GeneralCommandType.DISPLAY_CONTENT" to supportedCommands in capabilities to receive
	// these messages. Otherwise this function is never called.
	private fun onDisplayContent(itemId: UUID) {
		val playbackController = playbackControllerContainer.playbackController

		if (playbackController?.isPlaying == true || playbackController?.isPaused == true) {
			Timber.i("Not launching $itemId: playback in progress")
			return
		}

		Timber.i("Launching $itemId")

		coroutineScope.launch {
			val item by api.userLibraryApi.getItem(itemId = itemId)
			// ItemLauncher.launch(item)
		}
	}

	private fun onDisplayMessage(header: String?, text: String?) {
		val toastMessage = buildString {
			if (!header.isNullOrBlank()) append(header, ": ")
			append(text)
		}

		runBlocking(Dispatchers.Main) {
			Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
		}
	}

	companion object {
		const val TICKS_TO_MS = 10000L
		const val REWIND_MS = -11000
		const val FORWARD_MS = 30000
	}
}
