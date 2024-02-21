package org.jellyfin.androidtv.data.eventhandling

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.sockets.SocketInstance
import org.jellyfin.sdk.api.sockets.SocketInstanceState
import org.jellyfin.sdk.api.sockets.addGeneralCommandsListener
import org.jellyfin.sdk.api.sockets.addListener
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.LibraryUpdateInfo
import org.jellyfin.sdk.model.api.PlaystateCommand
import org.jellyfin.sdk.model.constant.MediaType
import org.jellyfin.sdk.model.extensions.get
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
	private val socketInstance: SocketInstance,
	private val dataRefreshService: DataRefreshService,
	private val mediaManager: MediaManager,
	private val playbackControllerContainer: PlaybackControllerContainer,
	private val navigationRepository: NavigationRepository,
	private val audioManager: AudioManager,
) {
	private val coroutineScope = CoroutineScope(Dispatchers.IO)
	val state = socketInstance.state

	suspend fun updateSession() {
		try {
			api.sessionApi.postCapabilities(
				playableMediaTypes = listOf(MediaType.Video, MediaType.Audio),
				supportsMediaControl = true,
				supportedCommands = buildList {
					add(GeneralCommandType.DISPLAY_CONTENT)
					add(GeneralCommandType.SET_SUBTITLE_STREAM_INDEX)
					add(GeneralCommandType.SET_AUDIO_STREAM_INDEX)

					add(GeneralCommandType.DISPLAY_MESSAGE)
					add(GeneralCommandType.SEND_STRING)

					// Note: These are used in the PlaySessionSocketService
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !audioManager.isVolumeFixed) {
						add(GeneralCommandType.VOLUME_UP)
						add(GeneralCommandType.VOLUME_DOWN)
						add(GeneralCommandType.SET_VOLUME)

						add(GeneralCommandType.MUTE)
						add(GeneralCommandType.UNMUTE)
						add(GeneralCommandType.TOGGLE_MUTE)
					}
				},
			)
		} catch (err: ApiClientException) {
			Timber.e(err, "Unable to update capabilities")
			return
		}

		val isOffline = state.value == SocketInstanceState.DISCONNECTED || state.value == SocketInstanceState.ERROR
		if (isOffline) socketInstance.reconnect()
		else socketInstance.updateCredentials()
	}

	init {
		socketInstance.apply {
			// Library
			addListener<LibraryChangedMessage> { message -> onLibraryChanged(message.info) }

			// Media playback
			addListener<PlayMessage> { message -> onPlayMessage(message) }
			addListener<PlayStateMessage> { message -> onPlayStateMessage(message) }

			addGeneralCommandsListener(setOf(GeneralCommandType.SET_SUBTITLE_STREAM_INDEX)) { message ->
				val index = message["index"]?.toIntOrNull() ?: return@addGeneralCommandsListener

				coroutineScope.launch(Dispatchers.Main) {
					playbackControllerContainer.playbackController?.switchSubtitleStream(index)
				}
			}

			addGeneralCommandsListener(setOf(GeneralCommandType.SET_AUDIO_STREAM_INDEX)) { message ->
				val index = message["index"]?.toIntOrNull() ?: return@addGeneralCommandsListener

				coroutineScope.launch(Dispatchers.Main) {
					playbackControllerContainer.playbackController?.switchAudioStream(index)
				}
			}

			// General commands
			addGeneralCommandsListener(setOf(GeneralCommandType.DISPLAY_CONTENT)) { message ->
				val itemId by message
				val itemType by message

				val itemUuid = itemId?.toUUIDOrNull()
				val itemKind = itemType?.let { type -> BaseItemKind.values().find { value -> value.serialName.equals(type, true) } }

				if (itemUuid != null && itemKind != null) onDisplayContent(itemUuid, itemKind)
			}
			addGeneralCommandsListener(setOf(GeneralCommandType.DISPLAY_MESSAGE, GeneralCommandType.SEND_STRING)) { message ->
				val header by message
				val text by message
				val string by message

				onDisplayMessage(header, text ?: string)
			}
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

		runCatching {
			PlaybackHelper.retrieveAndPlay(
				itemId.toString(),
				false,
				message.request.startPositionTicks,
				context
			)
		}.onFailure { Timber.w(it, "Failed to start playback") }
	}

	@Suppress("ComplexMethod")
	private fun onPlayStateMessage(message: PlayStateMessage) = coroutineScope.launch(Dispatchers.Main) {
		Timber.i("Received PlayStateMessage with command ${message.request.command}")

		// Audio playback uses (Rewrite)MediaManager, (legacy) video playback uses playbackController
		when {
			mediaManager.hasAudioQueueItems() -> {
				Timber.i("Ignoring PlayStateMessage: should be handled by PlaySessionSocketService")
				return@launch
			}

			// PlaybackController
			else -> {
				val playbackController = playbackControllerContainer.playbackController
				when (message.request.command) {
					PlaystateCommand.STOP -> playbackController?.endPlayback(true)
					PlaystateCommand.PAUSE, PlaystateCommand.UNPAUSE, PlaystateCommand.PLAY_PAUSE -> playbackController?.playPause()
					PlaystateCommand.NEXT_TRACK -> playbackController?.next()
					PlaystateCommand.PREVIOUS_TRACK -> playbackController?.prev()
					PlaystateCommand.SEEK -> playbackController?.seek(
						(message.request.seekPositionTicks ?: 0) / TICKS_TO_MS
					)

					PlaystateCommand.REWIND -> playbackController?.rewind()
					PlaystateCommand.FAST_FORWARD -> playbackController?.fastForward()
				}
			}
		}
	}

	private fun onDisplayContent(itemId: UUID, itemKind: BaseItemKind) = coroutineScope.launch(Dispatchers.Main) {
		val playbackController = playbackControllerContainer.playbackController

		if (playbackController?.isPlaying == true || playbackController?.isPaused == true) {
			Timber.i("Not launching $itemId: playback in progress")
			return@launch
		}

		Timber.i("Launching $itemId")

		when (itemKind) {
			BaseItemKind.USER_VIEW,
			BaseItemKind.COLLECTION_FOLDER -> coroutineScope.launch {
				val item by api.userLibraryApi.getItem(itemId = itemId)
				ItemLauncher.launchUserView(item)
			}

			else -> navigationRepository.navigate(Destinations.itemDetails(itemId))
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
	}
}
