package org.jellyfin.androidtv.syncplay

import android.os.SystemClock
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.api.sockets.subscribeSyncPlayCommands
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import org.jellyfin.sdk.model.api.SyncPlayPlayQueueUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupUpdateMessage
import timber.log.Timber

class SyncPlaySocketHandler(
	private val api: ApiClient,
	private val repository: SyncPlayRepository,
	private val playbackControllerContainer: PlaybackControllerContainer,
	private val videoQueueManager: VideoQueueManager,
	private val navigationRepository: NavigationRepository,
	private val userPreferences: UserPreferences,
	private val lifecycle: Lifecycle,
	private val loopGuard: SyncPlayLoopGuard = SyncPlayLoopGuard(nowMs = { SystemClock.elapsedRealtime() }),
	startListening: Boolean = true,
) {
	companion object {
		private const val LOG_TAG = "SyncPlaySocket"
	}

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	init {
		Timber.d("%s started (lifecycle=%s)", LOG_TAG, lifecycle.currentState)
		if (startListening) {
			scope.launch {
				runCatching { subscribe(this) }
					.onFailure { Timber.e(it, "%s failed to subscribe to websocket streams", LOG_TAG) }
			}
		}
	}

	private fun subscribe(coroutineScope: CoroutineScope) = api.webSocket.apply {
		subscribeSyncPlayCommands()
			.onEach { message ->
				runCatching {
					val command = message.data ?: return@runCatching
					logCommandTiming(command)
					Timber.d("%s command: %s", LOG_TAG, command)
					repository.handleCommand(command)
					applyCommand(command, coroutineScope)
				}.onFailure {
					Timber.e(it, "%s failed while processing command message", LOG_TAG)
				}
			}
			.launchIn(coroutineScope)

		subscribe<SyncPlayGroupUpdateMessage>()
			.onEach { message ->
				runCatching {
					val update = message.data
					Timber.d("%s update: %s", LOG_TAG, update)
					repository.handleGroupUpdate(update)
					if (update is SyncPlayPlayQueueUpdate) {
						applyQueueUpdate(update, coroutineScope)
					}
				}.onFailure {
					Timber.e(it, "%s failed while processing group update message", LOG_TAG)
				}
			}
			.launchIn(coroutineScope)
	}

	private fun applyCommand(command: SendCommand, coroutineScope: CoroutineScope) {
		val controller = playbackControllerContainer.playbackController ?: run {
			Timber.d("%s applyCommand ignored: no active playback controller", LOG_TAG)
			return
		}
		coroutineScope.launch(Dispatchers.Main) {
			Timber.d(
				"%s apply command=%s paused=%s item=%s",
				LOG_TAG,
				command.command,
				controller.isPaused(),
				controller.currentlyPlayingItem?.id,
			)
			repository.withRemoteCommand {
				when (command.command) {
					SendCommandType.PAUSE -> if (!controller.isPaused()) controller.pause()
					SendCommandType.UNPAUSE -> if (controller.isPaused()) controller.playPause()
					SendCommandType.SEEK -> {
						val ticks = command.positionTicks ?: run {
							Timber.w("%s applyCommand SEEK ignored: null ticks", LOG_TAG)
							return@withRemoteCommand
						}
						controller.seek(ticks / 10000)
					}
					SendCommandType.STOP -> controller.stop()
				}
			}
		}
	}

	private fun applyQueueUpdate(update: SyncPlayPlayQueueUpdate, coroutineScope: CoroutineScope) {
		val queue = update.data
		if (queue.playlist.isEmpty()) {
			Timber.d("%s queue update ignored: empty playlist", LOG_TAG)
			return
		}

		val safeIndex = queue.playingItemIndex.coerceIn(0, queue.playlist.lastIndex)
		val playlistItemId = queue.playlist[safeIndex].playlistItemId
		val queueStateKey = SyncPlayLoopGuard.QueueStateKey(
			playlistItemId = playlistItemId,
			index = safeIndex,
			startPositionTicks = queue.startPositionTicks,
			isPlaying = queue.isPlaying,
		)
		if (!loopGuard.shouldProcessQueueState(queueStateKey)) {
			Timber.d(
				"%s queue update ignored: duplicate playlistItemId=%s index=%s startTicks=%s isPlaying=%s",
				LOG_TAG,
				playlistItemId,
				safeIndex,
				queue.startPositionTicks,
				queue.isPlaying,
			)
			return
		}

		Timber.d(
			"%s queue update: items=%s index=%s startTicks=%s isPlaying=%s",
			LOG_TAG,
			queue.playlist.size,
			queue.playingItemIndex,
			queue.startPositionTicks,
			queue.isPlaying,
		)

		coroutineScope.launch(Dispatchers.IO) {
			val playlistIds = queue.playlist.map { it.itemId }
			val items = loadQueueItemsChunked(playlistIds) ?: run {
				Timber.w("%s failed to load queue items", LOG_TAG)
				return@launch
			}

			if (items.isEmpty()) {
				Timber.w("%s queue update returned 0 items from server", LOG_TAG)
				return@launch
			}
			val itemsById = items.associateBy { it.id }
			val orderedItems = playlistIds.mapNotNull { itemsById[it] }
			if (orderedItems.isEmpty()) {
				Timber.w("%s queue update produced empty ordered list", LOG_TAG)
				return@launch
			}

			val safeIndex = queue.playingItemIndex.coerceIn(0, orderedItems.size - 1)
			val positionMs = (queue.startPositionTicks / 10000L)
				.coerceAtLeast(0L)
				.coerceAtMost(Int.MAX_VALUE.toLong())
				.toInt()
			val targetItem = orderedItems[safeIndex]
			Timber.d(
				"%s target item=%s index=%s positionMs=%s isPlaying=%s",
				LOG_TAG,
				targetItem.id,
				safeIndex,
				positionMs,
				queue.isPlaying,
			)

			val existingController = playbackControllerContainer.playbackController
			if (existingController != null && existingController.currentlyPlayingItem?.id == targetItem.id) {
				Timber.d("%s reusing existing controller for item=%s", LOG_TAG, targetItem.id)
				withContext(Dispatchers.Main) {
					repository.withRemoteCommand {
						existingController.seek(positionMs.toLong())
						if (queue.isPlaying) {
							if (existingController.isPaused()) existingController.playPause()
						} else if (!existingController.isPaused()) {
							existingController.pause()
						}
					}
				}
				sendReady(queue, safeIndex)
				return@launch
			}

			withContext(Dispatchers.Main) {
				videoQueueManager.setCurrentVideoQueue(orderedItems)
				videoQueueManager.setCurrentMediaPosition(safeIndex)
				val destination = if (userPreferences[UserPreferences.playbackRewriteVideoEnabled]) {
					Destinations.videoPlayerNew(positionMs)
				} else {
					Destinations.videoPlayer(positionMs)
				}
				Timber.d("%s navigating to %s", LOG_TAG, destination)
				navigationRepository.navigate(destination, replace = true)
			}

			val controller = awaitPlaybackController() ?: return@launch
			Timber.d("%s controller ready for item=%s", LOG_TAG, targetItem.id)
			withContext(Dispatchers.Main) {
				repository.withRemoteCommand {
					controller.seek(positionMs.toLong())
					if (queue.isPlaying) {
						if (controller.isPaused()) controller.playPause()
					} else if (!controller.isPaused()) {
						controller.pause()
					}
				}
			}
			sendReady(queue, safeIndex)
		}
	}

	private suspend fun loadQueueItemsChunked(
		playlistIds: List<java.util.UUID>,
		chunkSize: Int = 50,
	): List<org.jellyfin.sdk.model.api.BaseItemDto>? {
		if (playlistIds.isEmpty()) return emptyList()
		val chunks = playlistIds.chunked(chunkSize)
		Timber.d(
			"%s loading %s items in %s chunks (chunkSize=%s)",
			LOG_TAG,
			playlistIds.size,
			chunks.size,
			chunkSize,
		)
		val items = ArrayList<org.jellyfin.sdk.model.api.BaseItemDto>(playlistIds.size)
		for ((index, chunk) in chunks.withIndex()) {
			val result = runCatching {
				api.itemsApi.getItems(
					ids = chunk,
					fields = ItemRepository.itemFields,
				).content.items
			}
			val chunkItems = result.getOrElse {
				Timber.w(
					it,
					"%s failed to load queue items (chunk %s/%s)",
					LOG_TAG,
					index + 1,
					chunks.size,
				)
				return null
			}
			items.addAll(chunkItems)
		}
		return items
	}

	private suspend fun awaitPlaybackController(): org.jellyfin.androidtv.ui.playback.PlaybackController? {
		repeat(50) {
			playbackControllerContainer.playbackController?.let { return it }
			delay(100)
		}
		Timber.w("%s timed out waiting for playback controller", LOG_TAG)
		return null
	}

	private fun sendReady(queue: org.jellyfin.sdk.model.api.PlayQueueUpdate, index: Int) {
		val playlistItemId = queue.playlist.getOrNull(index)?.playlistItemId ?: return
		val readyState = SyncPlayLoopGuard.ReadyStateKey(
			playlistItemId = playlistItemId,
			positionTicks = queue.startPositionTicks,
			isPlaying = queue.isPlaying,
		)
		if (!loopGuard.shouldSendReady(readyState)) {
			Timber.d(
				"%s sendReady skipped: duplicate playlistItemId=%s positionTicks=%s isPlaying=%s",
				LOG_TAG,
				playlistItemId,
				queue.startPositionTicks,
				queue.isPlaying,
			)
			return
		}
		Timber.d(
			"%s sendReady playlistItemId=%s positionTicks=%s isPlaying=%s",
			LOG_TAG,
			playlistItemId,
			queue.startPositionTicks,
			queue.isPlaying,
		)
		repository.sendReady(playlistItemId, queue.startPositionTicks, queue.isPlaying)
	}

	private fun logCommandTiming(command: SendCommand) {
		val nowEpochMs = System.currentTimeMillis()
		val commandWhenMs = command.`when`.toEpochMs()
		val emittedAtMs = command.emittedAt.toEpochMs()
		val schedulingDelayMs = nowEpochMs - commandWhenMs
		val transitDelayMs = nowEpochMs - emittedAtMs

		Timber.d(
			"%s command timing command=%s schedulingDelayMs=%s transitDelayMs=%s when=%s emittedAt=%s",
			LOG_TAG,
			command.command,
			schedulingDelayMs,
			transitDelayMs,
			command.`when`,
			command.emittedAt,
		)
	}

	private fun LocalDateTime.toEpochMs(): Long =
		atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
