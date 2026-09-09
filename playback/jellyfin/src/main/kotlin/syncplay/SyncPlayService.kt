package org.jellyfin.playback.jellyfin.syncplay

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jellyfin.playback.core.PlayerCommand
import org.jellyfin.playback.core.PlayerCommandHandler
import org.jellyfin.playback.core.backend.PlayerBackendEventListener
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.model.RepeatMode
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.InitialPlayback
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.initialPlayback
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import org.jellyfin.playback.jellyfin.queue.createBaseItemQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.syncPlayApi
import org.jellyfin.sdk.api.client.extensions.timeSyncApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.sockets.SocketApiState
import org.jellyfin.sdk.model.api.BufferRequestDto
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupQueueMode
import org.jellyfin.sdk.model.api.GroupRepeatMode
import org.jellyfin.sdk.model.api.GroupShuffleMode
import org.jellyfin.sdk.model.api.GroupUpdate
import org.jellyfin.sdk.model.api.JoinGroupRequestDto
import org.jellyfin.sdk.model.api.NewGroupRequestDto
import org.jellyfin.sdk.model.api.NextItemRequestDto
import org.jellyfin.sdk.model.api.PingRequestDto
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import org.jellyfin.sdk.model.api.PlayQueueUpdateReason
import org.jellyfin.sdk.model.api.PlayRequestDto
import org.jellyfin.sdk.model.api.PreviousItemRequestDto
import org.jellyfin.sdk.model.api.QueueRequestDto
import org.jellyfin.sdk.model.api.ReadyRequestDto
import org.jellyfin.sdk.model.api.RemoveFromPlaylistRequestDto
import org.jellyfin.sdk.model.api.SeekRequestDto
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import org.jellyfin.sdk.model.api.SetPlaylistItemRequestDto
import org.jellyfin.sdk.model.api.SetRepeatModeRequestDto
import org.jellyfin.sdk.model.api.SetShuffleModeRequestDto
import org.jellyfin.sdk.model.api.SyncPlayCommandMessage
import org.jellyfin.sdk.model.api.SyncPlayGroupDoesNotExistUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupJoinedUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupLeftUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupUpdateMessage
import org.jellyfin.sdk.model.api.SyncPlayLibraryAccessDeniedUpdate
import org.jellyfin.sdk.model.api.SyncPlayNotInGroupUpdate
import org.jellyfin.sdk.model.api.SyncPlayPlayQueueUpdate
import org.jellyfin.sdk.model.api.SyncPlayStateUpdate
import org.jellyfin.sdk.model.api.SyncPlayUserJoinedUpdate
import org.jellyfin.sdk.model.api.SyncPlayUserLeftUpdate
import org.jellyfin.sdk.model.extensions.ticks
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The same server-authoritative SyncPlay protocol used by Jellyfin Web. User commands are sent
 * to the group; scheduled server commands go directly to the backend, avoiding command echoes.
 * All player and group state is confined to the main dispatcher.
 */
@Suppress("TooManyFunctions", "LargeClass")
class SyncPlayService(
	private val api: ApiClient,
	private val clock: SyncPlayClock = SyncPlayClock(),
) : PlayerService(), PlayerCommandHandler {
	private val _group = MutableStateFlow<GroupInfoDto?>(null)
	val group = _group.asStateFlow()
	private val _groups = MutableStateFlow<List<GroupInfoDto>>(emptyList())
	val groups = _groups.asStateFlow()
	private val _busy = MutableStateFlow(false)
	val busy = _busy.asStateFlow()
	private val _error = MutableStateFlow<String?>(null)
	val error = _error.asStateFlow()
	private val _syncCorrectionEnabled = MutableStateFlow(false)
	val syncCorrectionEnabled = _syncCorrectionEnabled.asStateFlow()
	private val _queueState = MutableStateFlow<PlayQueueUpdate?>(null)
	val queueState = _queueState.asStateFlow()

	private val initialized = CompletableDeferred<Unit>()
	private val operationMutex = Mutex()
	private val requestMutex = Mutex()
	private val driftPolicy = SyncPlayDriftPolicy()
	private var generation = 0L
	private var playbackGeneration = 0L
	private var requestsJob: Job? = null
	private var joining = false
	private var joiningRequestSent = false
	private var joinResult: CompletableDeferred<GroupInfoDto>? = null
	private var sessionIdentity: Pair<String?, String?>? = null
	private var playQueue: PlayQueueUpdate?
		get() = _queueState.value
		set(value) { _queueState.value = value }
	private var currentEntry: QueueEntry? = null
	private var entries = mutableMapOf<UUID, QueueEntry>()
	private var awaitingReady = false
	private var buffering = false
	private var reportedBuffering = false
	private var commandJob: Job? = null
	private var bufferJob: Job? = null
	private var correctionJob: Job? = null
	private var timeSyncJob: Job? = null
	private var lastCommand: SendCommand? = null
	private var joinedAt: LocalDateTime? = null
	private var playingAnchor: SendCommand? = null
	private var unpausedAt = 0L

	private val playlistItemId: UUID?
		get() = playQueue?.let { it.playlist.getOrNull(it.playingItemIndex)?.playlistItemId }

	val active: Boolean get() = group.value != null || joining

	@Suppress("TooGenericExceptionCaught") // A failed protocol handler must not kill the socket collector.
	override suspend fun onInitialize() = withContext(Dispatchers.Main.immediate) {
		manager.addBackendListener(backendListener)
		coroutineScope.launch(Dispatchers.Main.immediate, start = CoroutineStart.UNDISPATCHED) {
			try {
				awaitCancellation()
			} finally {
				withContext(NonCancellable + Dispatchers.Main.immediate) {
					manager.removeBackendListener(backendListener)
					if (active) reset(stopPlayback = true)
				}
			}
		}
		// One subscription preserves ordering between queue updates and playback commands.
		coroutineScope.launch(Dispatchers.Main.immediate, start = CoroutineStart.UNDISPATCHED) {
			api.webSocket.subscribeAll().collect { message ->
				try {
					if (sessionIdentity != null && sessionIdentity != identity()) reset(stopPlayback = true)
					when (message) {
						is SyncPlayGroupUpdateMessage -> receiveGroupUpdate(message.data)
						is SyncPlayCommandMessage -> message.data?.let(::receiveCommand)
						else -> Unit
					}
				} catch (exception: CancellationException) {
					throw exception
				} catch (exception: Exception) {
					Timber.w(exception, "SyncPlay update failed")
					failAndLeave("Unable to play this SyncPlay group.")
				}
			}
		}
		coroutineScope.launch(Dispatchers.Main.immediate) {
			api.webSocket.state.collect { socketState ->
				if (socketState is SocketApiState.Disconnected && active) {
					failAndLeave("SyncPlay disconnected. Join the group again to resume.")
				}
			}
		}
		initialized.complete(Unit)
		Unit
	}

	fun clearError() { _error.value = null }

	fun isGroupEntry(entry: QueueEntry?): Boolean = group.value != null && entry != null && entry === currentEntry

	fun setSyncCorrectionEnabled(enabled: Boolean) {
		_syncCorrectionEnabled.value = enabled
		if (!enabled) resetCorrection()
	}

	suspend fun refreshGroups() = operation("Unable to load SyncPlay groups.") {
		val expectedGeneration = generation
		val expectedIdentity = identity()
		val available = api.syncPlayApi.syncPlayGetGroups().content
		if (generation != expectedGeneration || identity() != expectedIdentity) return@operation
		_groups.value = available
		group.value?.groupId?.let { id -> _groups.value.firstOrNull { it.groupId == id }?.let { _group.value = it } }
	}

	suspend fun createGroup(name: String) = operation("Unable to create a SyncPlay group. Check your SyncPlay permissions.") {
		require(name.isNotBlank())
		beginJoin()
		val expectedGeneration = generation
		joiningRequestSent = true
		val created = api.syncPlayApi.syncPlayCreateGroup(NewGroupRequestDto(name.trim())).content
		if (expectedGeneration == generation && sessionIdentity == identity()) acceptGroup(created)
	}

	suspend fun joinGroup(id: UUID) = operation("Unable to join this SyncPlay group. It may have closed or access may be denied.") {
		beginJoin()
		val result = requireNotNull(joinResult)
		joiningRequestSent = true
		api.syncPlayApi.syncPlayJoinGroup(JoinGroupRequestDto(id))
		withTimeout(JOIN_TIMEOUT_MILLIS) { require(result.await().groupId == id) }
	}

	suspend fun leaveGroup() = operation("Unable to leave the SyncPlay group.") {
		val wasJoined = group.value != null || joining
		reset(stopPlayback = false)
		if (wasJoined) api.syncPlayApi.syncPlayLeaveGroup()
	}

	/** Called before changing server/user credentials; do not send requests with the next session. */
	suspend fun endSession() = withContext(Dispatchers.Main.immediate) {
		val wasJoined = group.value != null || joining
		reset(stopPlayback = true)
		_groups.value = emptyList()
		_error.value = null
		if (wasJoined) {
			try {
				withTimeout(LEAVE_TIMEOUT_MILLIS) { api.syncPlayApi.syncPlayLeaveGroup() }
			} catch (_: TimeoutCancellationException) {
				// Logout must still finish when the old server is unreachable.
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				// The server also removes this session when its socket closes.
			}
		}
	}

	/** Start a title/playlist selected while grouped, instead of replacing only the TV's queue. */
	suspend fun playItems(ids: List<UUID>, index: Int, position: Duration) = operation("Unable to start group playback.") {
		require(group.value != null && ids.isNotEmpty() && index in ids.indices)
		api.syncPlayApi.syncPlaySetNewQueue(PlayRequestDto(ids, index, position.inWholeTicks))
	}

	/** Add media to the shared playlist without changing the TV's local queue ahead of the server. */
	fun queueItems(ids: List<UUID>) {
		if (group.value == null || ids.isEmpty()) return
		launchRequest { api.syncPlayApi.syncPlayQueue(QueueRequestDto(ids, GroupQueueMode.QUEUE)) }
	}

	private suspend fun beginJoin() {
		if (group.value != null) api.syncPlayApi.syncPlayLeaveGroup()
		reset(stopPlayback = false)
		joining = true
		joinResult = CompletableDeferred()
		sessionIdentity = identity()
		withTimeout(JOIN_TIMEOUT_MILLIS) { api.webSocket.state.first { it is SocketApiState.Connected } }
		measureTime()
	}

	private fun acceptGroup(info: GroupInfoDto) {
		if (!joining && group.value?.groupId != info.groupId) return
		val changed = group.value?.groupId != info.groupId
		if (changed) {
			// Local automatic repeat/order must stay disabled while the group owns the queue.
			state.setRepeatMode(RepeatMode.NONE)
			state.setPlaybackOrder(PlaybackOrder.DEFAULT)
		}
		_group.value = info
		joining = false
		joiningRequestSent = false
		joinResult?.complete(info)
		joinResult = null
		if (!changed) return
		joinedAt = info.lastUpdatedAt
		requestsJob = SupervisorJob(coroutineScope.coroutineContext[Job])
		manager.backend.setSpeed(1f)
		timeSyncJob?.cancel()
		timeSyncJob = coroutineScope.launch(Dispatchers.Main.immediate) {
			var samples = 1
			while (group.value != null) {
				delay(if (samples < INITIAL_TIME_SAMPLES) INITIAL_TIME_INTERVAL else TIME_INTERVAL)
				try {
					measureTime()
					api.syncPlayApi.syncPlayPing(PingRequestDto(clock.pingMillis))
					samples++
				} catch (exception: CancellationException) {
					throw exception
				} catch (_: Exception) {
					// Retain the last good clock estimate; socket loss tears down the group separately.
				}
			}
		}
		startCorrectionLoop()
	}

	private suspend fun measureTime() {
		val measurement = clock.beginMeasurement()
		val response = api.timeSyncApi.getUtcTime().content
		clock.completeMeasurement(measurement, response.requestReceptionTime.epochMillis, response.responseTransmissionTime.epochMillis)
	}

	@Suppress("CyclomaticComplexMethod") // Keep the complete server group-update dispatch in one place.
	private suspend fun receiveGroupUpdate(update: GroupUpdate) {
		when (update) {
			is SyncPlayNotInGroupUpdate -> {
				if (joining || group.value != null) {
					reset(stopPlayback = true)
					_error.value = "You are no longer in this SyncPlay group."
				}
				return
			}
			is SyncPlayGroupDoesNotExistUpdate -> {
				if (joining || group.value != null) failAndLeave("This SyncPlay group no longer exists.")
				return
			}
			is SyncPlayLibraryAccessDeniedUpdate -> {
				if (joining || group.value != null) failAndLeave("You do not have access to the group's current media.")
				return
			}
			else -> Unit
		}
		if (update is SyncPlayGroupJoinedUpdate) {
			acceptGroup(update.data)
			return
		}
		if (update.groupId != group.value?.groupId) return
		when (update) {
			is SyncPlayPlayQueueUpdate -> receiveQueue(update.data)
			is SyncPlayGroupLeftUpdate -> reset(stopPlayback = false)
			is SyncPlayStateUpdate -> _group.value = group.value?.copy(state = update.data.state)
			is SyncPlayUserJoinedUpdate, is SyncPlayUserLeftUpdate -> {
				refreshJoinedGroup(update.groupId)
			}
			is SyncPlayGroupJoinedUpdate, is SyncPlayNotInGroupUpdate,
			is SyncPlayGroupDoesNotExistUpdate, is SyncPlayLibraryAccessDeniedUpdate -> Unit
		}
	}

	/** Participant metadata must not hold up the ordered queue and playback command stream. */
	@Suppress("TooGenericExceptionCaught") // Participant metadata is an independent, best-effort network request.
	private fun refreshJoinedGroup(groupId: UUID) {
		val expectedGeneration = generation
		val expectedIdentity = identity()
		coroutineScope.launch(Dispatchers.Main.immediate) {
			try {
				val info = api.syncPlayApi.syncPlayGetGroups().content.firstOrNull { it.groupId == groupId } ?: return@launch
				if (generation != expectedGeneration || identity() != expectedIdentity) return@launch
				val current = group.value ?: return@launch
				if (current.groupId == groupId && info.lastUpdatedAt >= current.lastUpdatedAt) _group.value = info
			} catch (exception: CancellationException) {
				throw exception
			} catch (exception: Exception) {
				Timber.w(exception, "Unable to refresh SyncPlay participants")
			}
		}
	}

	private suspend fun receiveQueue(update: PlayQueueUpdate) {
		val previous = playQueue
		if (previous != null && update.lastUpdate < previous.lastUpdate) return
		val next = update.playlist.getOrNull(update.playingItemIndex)
		val sameItem = next?.playlistItemId == playlistItemId && currentEntry != null
		playQueue = update
		if (next == null) {
			clearPlayback()
			return
		}
		// A queue edit around the current item must not restart it, but indices still need updating.
		val metadataOnly = update.reason in setOf(
			PlayQueueUpdateReason.MOVE_ITEM, PlayQueueUpdateReason.QUEUE, PlayQueueUpdateReason.QUEUE_NEXT,
			PlayQueueUpdateReason.REPEAT_MODE, PlayQueueUpdateReason.SHUFFLE_MODE, PlayQueueUpdateReason.REMOVE_ITEMS,
		)
		if (sameItem && metadataOnly) {
			entries.keys.retainAll(update.playlist.map { it.playlistItemId }.toSet())
			synchronizeQueue(update)
			return
		}
		playbackGeneration++
		lastCommand = null
		cancelScheduledCommand()
		currentEntry = null
		bufferJob?.cancel()
		val expectedGeneration = generation
		val item = api.userLibraryApi.getItem(next.itemId).content
		if (generation != expectedGeneration) return
		val entry = createBaseItemQueueEntry(api, item).also {
			it.initialPlayback = InitialPlayback(update.startPositionTicks.ticks, playWhenReady = false)
		}
		entries = mutableMapOf()
		entries[next.playlistItemId] = entry
		currentEntry = entry
		awaitingReady = true
		buffering = true
		reportedBuffering = false
		manager.backend.pause()
		synchronizeQueue(update)
	}

	private suspend fun synchronizeQueue(update: PlayQueueUpdate) {
		val queueEntries = entries
		manager.queue.synchronize(object : QueueSupplier {
			override val size = update.playlist.size
			override suspend fun getItem(index: Int): QueueEntry? {
				val queueItem = update.playlist.getOrNull(index) ?: return null
				return queueEntries[queueItem.playlistItemId] ?: createBaseItemQueueEntry(
					api, api.userLibraryApi.getItem(queueItem.itemId).content
				).also { queueEntries[queueItem.playlistItemId] = it }
			}
		}, update.playingItemIndex)
	}

	@Suppress("CyclomaticComplexMethod", "ReturnCount") // Guard stale/duplicate commands before the four protocol actions.
	private fun receiveCommand(command: SendCommand) {
		if (command.groupId != group.value?.groupId) return
		if (command.command != SendCommandType.STOP && command.playlistItemId != playlistItemId) return
		if (command.emittedAt < (joinedAt ?: command.emittedAt)) return
		if (lastCommand?.let { command.emittedAt < it.emittedAt } == true) return
		val duplicate = lastCommand?.let {
			it.command == command.command && it.`when` == command.`when` &&
				it.positionTicks == command.positionTicks && it.playlistItemId == command.playlistItemId
		} == true
		lastCommand = command
		if (duplicate) {
			if (clock.delayUntil(command.`when`.epochMillis) > 0) return
			val isPlaying = state.playState.value == PlayState.PLAYING
			val atPosition = state.positionInfo.active.inWholeTicks == (command.positionTicks ?: 0)
			when (command.command) {
				SendCommandType.UNPAUSE -> if (isPlaying) return
				SendCommandType.PAUSE -> if (!isPlaying && atPosition) return
				SendCommandType.SEEK -> if (!isPlaying && atPosition && !buffering) {
					reportReady()
					return
				}
				SendCommandType.STOP -> if (currentEntry == null) return
			}
		}
		cancelScheduledCommand()
		val expectedGeneration = generation
		commandJob = coroutineScope.launch(Dispatchers.Main.immediate) {
			delay(clock.delayUntil(command.`when`.epochMillis))
			if (generation != expectedGeneration) return@launch
			val position = (command.positionTicks ?: 0).ticks
			when (command.command) {
				SendCommandType.UNPAUSE -> {
					val target = position + (clock.nowMillis() - command.`when`.epochMillis).coerceAtLeast(0).milliseconds
					if (abs((state.positionInfo.active - target).inWholeMilliseconds) >= SEEK_TOLERANCE_MILLIS) manager.backend.seekTo(target)
					playingAnchor = command
					unpausedAt = clock.monotonicMillis()
					manager.backend.play()
				}
				SendCommandType.PAUSE -> {
					manager.backend.pause()
					manager.backend.seekTo(position)
				}
				SendCommandType.SEEK -> {
					playbackGeneration++
					awaitingReady = true
					manager.backend.pause()
					manager.backend.seekTo(position)
					// ExoPlayer may keep STATE_READY when seeking inside its buffer.
					if (!buffering) reportReady()
				}
				SendCommandType.STOP -> clearPlayback()
			}
		}
	}

	@Suppress("CyclomaticComplexMethod") // Exhaustive mapping from local controls to server requests.
	override fun handleCommand(command: PlayerCommand): Boolean {
		if (group.value == null) return false
		val id = playlistItemId
		val selectedId = when (command) {
			is PlayerCommand.Select -> playQueue?.playlist?.getOrNull(command.index)?.playlistItemId
			is PlayerCommand.Remove -> playQueue?.playlist?.getOrNull(command.index)?.playlistItemId
			else -> null
		}
		launchRequest {
			when (command) {
				PlayerCommand.Play -> api.syncPlayApi.syncPlayUnpause()
				PlayerCommand.Pause -> api.syncPlayApi.syncPlayPause()
				PlayerCommand.Stop -> api.syncPlayApi.syncPlayStop()
				is PlayerCommand.Seek -> api.syncPlayApi.syncPlaySeek(SeekRequestDto(command.position.coerceAtLeast(Duration.ZERO).inWholeTicks))
				PlayerCommand.Next -> if (id != null) api.syncPlayApi.syncPlayNextItem(NextItemRequestDto(id))
				PlayerCommand.Previous -> if (id != null) api.syncPlayApi.syncPlayPreviousItem(PreviousItemRequestDto(id))
				is PlayerCommand.Select -> selectedId?.let {
					api.syncPlayApi.syncPlaySetPlaylistItem(SetPlaylistItemRequestDto(it))
				}
				is PlayerCommand.Speed -> Unit // The group timeline runs at 1x; speed is reserved for temporary correction.
				is PlayerCommand.Order -> api.syncPlayApi.syncPlaySetShuffleMode(SetShuffleModeRequestDto(
					if (command.order == PlaybackOrder.DEFAULT) GroupShuffleMode.SORTED else GroupShuffleMode.SHUFFLE
				))
				is PlayerCommand.Repeat -> api.syncPlayApi.syncPlaySetRepeatMode(SetRepeatModeRequestDto(
					if (command.mode == RepeatMode.NONE) GroupRepeatMode.REPEAT_NONE else GroupRepeatMode.REPEAT_ONE
				))
				is PlayerCommand.Remove -> selectedId?.let {
					api.syncPlayApi.syncPlayRemoveFromPlaylist(RemoveFromPlaylistRequestDto(listOf(it), false, false))
				}
			}
		}
		return true
	}

	private val backendListener = object : PlayerBackendEventListener() {
		override fun onBuffering(entry: QueueEntry, buffering: Boolean) {
			if (entry !== currentEntry || group.value == null) return
			this@SyncPlayService.buffering = buffering
			bufferJob?.cancel()
			if (!buffering || awaitingReady) return
			bufferJob = coroutineScope.launch(Dispatchers.Main.immediate) {
				delay(BUFFERING_DELAY_MILLIS)
				val id = playlistItemId ?: return@launch
				reportedBuffering = true
				val request = BufferRequestDto(now(), state.positionInfo.active.inWholeTicks, playingAnchor != null, id)
				launchRequest { api.syncPlayApi.syncPlayBuffering(request) }
			}
		}

		override fun onMediaStreamReady(entry: QueueEntry) {
			if (entry !== currentEntry || group.value == null) return
			buffering = false
			bufferJob?.cancel()
			if (awaitingReady || reportedBuffering) reportReady()
		}

		override fun onMediaStreamError(entry: QueueEntry) {
			if (entry !== currentEntry || group.value == null) return
			coroutineScope.launch(Dispatchers.Main.immediate) { failAndLeave("Unable to play the group's current media.") }
		}
	}

	private fun reportReady() {
		val id = playlistItemId ?: return
		awaitingReady = false
		reportedBuffering = false
		val request = ReadyRequestDto(now(), state.positionInfo.active.inWholeTicks, state.playState.value == PlayState.PLAYING, id)
		launchRequest { api.syncPlayApi.syncPlayReady(request) }
	}

	// Eligibility guards precede one drift-policy decision.
	@Suppress("CyclomaticComplexMethod", "ComplexCondition", "LoopWithTooManyJumpStatements")
	private fun startCorrectionLoop() {
		coroutineScope.launch(Dispatchers.Main.immediate) {
			val expectedGeneration = generation
			while (generation == expectedGeneration && group.value != null) {
				delay(driftPolicy.correctionIntervalMillis)
				if (generation != expectedGeneration || group.value == null) break
				val anchor = playingAnchor ?: continue
				if (buffering || awaitingReady || correctionJob?.isActive == true || state.playState.value != PlayState.PLAYING) continue
				if (clock.monotonicMillis() - unpausedAt < driftPolicy.unpauseGracePeriodMillis) continue
				val targetMillis = (anchor.positionTicks ?: 0).ticks.inWholeMilliseconds + clock.nowMillis() - anchor.`when`.epochMillis
				val drift = (targetMillis - state.positionInfo.active.inWholeMilliseconds).toDouble()
				when (val correction = driftPolicy.correction(drift, supportsPlaybackRate = true, enabled = syncCorrectionEnabled.value)) {
					SyncPlayCorrection.None -> Unit
					SyncPlayCorrection.Seek -> manager.backend.seekTo(targetMillis.coerceAtLeast(0).milliseconds)
					is SyncPlayCorrection.ChangeSpeed -> {
						manager.backend.setSpeed(correction.rate)
						correctionJob = coroutineScope.launch(Dispatchers.Main.immediate) {
							delay(correction.durationMillis)
							manager.backend.setSpeed(1f)
						}
					}
				}
			}
		}
	}

	@Suppress("TooGenericExceptionCaught") // All request failures detach locally; cancellation still propagates.
	private fun launchRequest(block: suspend () -> Unit) {
		val expectedGeneration = generation
		val expectedPlaybackGeneration = playbackGeneration
		val parent = requestsJob ?: return
		coroutineScope.launch(Dispatchers.Main.immediate + parent) {
			requestMutex.withLock {
				if (generation != expectedGeneration || playbackGeneration != expectedPlaybackGeneration) return@withLock
				if (group.value == null || identity() != sessionIdentity) return@withLock
				try {
					block()
				} catch (exception: CancellationException) {
					throw exception
				} catch (exception: Exception) {
					Timber.w(exception, "SyncPlay request failed")
					if (generation == expectedGeneration && playbackGeneration == expectedPlaybackGeneration && identity() == sessionIdentity) {
						failAndLeave("SyncPlay could not reach the server. Join the group again to resume.")
					}
				}
			}
		}
	}

	@Suppress("TooGenericExceptionCaught") // Convert network failures to visible operation errors, preserving cancellation.
	private suspend fun operation(message: String, block: suspend () -> Unit) = withContext(Dispatchers.Main.immediate) {
		initialized.await()
		operationMutex.withLock {
			_busy.value = true
			_error.value = null
			try {
				block()
			} catch (exception: TimeoutCancellationException) {
				Timber.w(exception, "SyncPlay operation timed out")
				abandonJoin()
				_error.value = message
			} catch (exception: CancellationException) {
				withContext(NonCancellable) { abandonJoin() }
				throw exception
			} catch (exception: Exception) {
				Timber.w(exception, "SyncPlay operation failed")
				abandonJoin()
				_error.value = message
			} finally {
				_busy.value = false
			}
		}
	}

	private suspend fun abandonJoin() {
		if (!joining) return
		val leave = joiningRequestSent && sessionIdentity == identity()
		reset(stopPlayback = false)
		if (leave) {
			try {
				withTimeout(LEAVE_TIMEOUT_MILLIS) { api.syncPlayApi.syncPlayLeaveGroup() }
			} catch (_: TimeoutCancellationException) {
				// Local state is already detached.
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				// Best-effort cleanup for a join whose acknowledgement never arrived.
			}
		}
	}

	private suspend fun failAndLeave(message: String) {
		val wasJoined = group.value != null || joiningRequestSent
		reset(stopPlayback = true)
		_error.value = message
		if (wasJoined) {
			// A failed playback request may be inside the request job that reset just cancelled.
			withContext(NonCancellable) {
				try { withTimeout(LEAVE_TIMEOUT_MILLIS) { api.syncPlayApi.syncPlayLeaveGroup() } } catch (_: Exception) { }
			}
		}
	}

	private fun resetCorrection() {
		correctionJob?.cancel()
		correctionJob = null
		manager.backend.setSpeed(1f)
	}

	private fun cancelScheduledCommand() {
		commandJob?.cancel()
		commandJob = null
		playingAnchor = null
		resetCorrection()
	}

	private fun clearPlayback() {
		playbackGeneration++
		cancelScheduledCommand()
		bufferJob?.cancel()
		currentEntry = null
		awaitingReady = false
		buffering = false
		reportedBuffering = false
		manager.backend.stop()
		manager.queue.clear()
	}

	private fun reset(stopPlayback: Boolean) {
		generation++
		playbackGeneration++
		requestsJob?.cancel()
		requestsJob = null
		joining = false
		joiningRequestSent = false
		joinResult?.completeExceptionally(IllegalStateException("SyncPlay join ended"))
		joinResult = null
		_group.value = null
		playQueue = null
		lastCommand = null
		joinedAt = null
		timeSyncJob?.cancel()
		bufferJob?.cancel()
		cancelScheduledCommand()
		if (stopPlayback) clearPlayback()
		currentEntry = null
		entries = mutableMapOf()
		sessionIdentity = null
		clock.reset()
		manager.backend.setSpeed(state.speed.value)
	}

	private fun identity() = api.baseUrl to api.accessToken
	private fun now(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.nowMillis()), ZoneOffset.UTC)
	private val LocalDateTime.epochMillis get() = toInstant(ZoneOffset.UTC).toEpochMilli()
	private val Duration.inWholeTicks get() = inWholeNanoseconds / NANOS_PER_TICK

	private companion object {
		const val NANOS_PER_TICK = 100L
		const val JOIN_TIMEOUT_MILLIS = 10_000L
		const val LEAVE_TIMEOUT_MILLIS = 3_000L
		const val BUFFERING_DELAY_MILLIS = 3_000L
		const val SEEK_TOLERANCE_MILLIS = 400L
		const val INITIAL_TIME_SAMPLES = 4
		const val INITIAL_TIME_INTERVAL = 1_000L
		const val TIME_INTERVAL = 60_000L
	}
}
