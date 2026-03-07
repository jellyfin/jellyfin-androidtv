package org.jellyfin.androidtv.syncplay

import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.syncPlayApi
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupStateUpdate
import org.jellyfin.sdk.model.api.GroupUpdate
import org.jellyfin.sdk.model.api.GroupUpdateType
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import org.jellyfin.sdk.model.api.PlayRequestDto
import org.jellyfin.sdk.model.api.ReadyRequestDto
import org.jellyfin.sdk.model.api.SeekRequestDto
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import org.jellyfin.sdk.model.api.SetPlaylistItemRequestDto
import org.jellyfin.sdk.model.api.SyncPlayGroupDoesNotExistUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupJoinedUpdate
import org.jellyfin.sdk.model.api.SyncPlayGroupLeftUpdate
import org.jellyfin.sdk.model.api.SyncPlayLibraryAccessDeniedUpdate
import org.jellyfin.sdk.model.api.SyncPlayNotInGroupUpdate
import org.jellyfin.sdk.model.api.SyncPlayPlayQueueUpdate
import timber.log.Timber

data class SyncPlayState(
	val groups: List<GroupInfoDto> = emptyList(),
	val activeGroup: GroupInfoDto? = null,
	val queueUpdate: PlayQueueUpdate? = null,
	val stateUpdate: GroupStateUpdate? = null,
	val lastError: String? = null,
)

interface SyncPlayRepository {
	val state: StateFlow<SyncPlayState>

	fun refreshGroups()
	fun createGroup(name: String)
	fun joinGroup(groupId: UUID)
	fun leaveGroup()
	fun syncCurrentPlayback(itemIds: List<UUID>, playingIndex: Int, positionTicks: Long, isPlaying: Boolean)
	fun sendPause(positionTicks: Long)
	fun sendUnpause(positionTicks: Long)
	fun sendSeek(positionTicks: Long)
	fun sendStop()
	fun sendReady(playlistItemId: UUID, positionTicks: Long, isPlaying: Boolean)

	fun handleGroupUpdate(update: GroupUpdate)
	fun handleCommand(command: SendCommand)
	fun withRemoteCommand(block: () -> Unit)
}

class SyncPlayRepositoryImpl(
	private val api: ApiClient,
) : SyncPlayRepository {
	companion object {
		private const val LOG_TAG = "SyncPlayRepo"
	}

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val suppressLocalCommands = AtomicBoolean(false)
	private val _state = MutableStateFlow(SyncPlayState())
	override val state: StateFlow<SyncPlayState> = _state

	private var pendingReady: PendingReady? = null

	override fun refreshGroups() {
		Timber.d("%s refreshGroups requested", LOG_TAG)
		scope.launch {
			runCatching {
				api.syncPlayApi.syncPlayGetGroups().content
			}.onSuccess { groups ->
				Timber.d("%s refreshGroups success: groupCount=%s", LOG_TAG, groups.size)
				_state.update { it.copy(groups = groups, lastError = null) }
			}.onFailure { err ->
				Timber.w(err, "%s refreshGroups failed", LOG_TAG)
				updateLastError(err.message ?: "SyncPlay group list failed")
			}
		}
	}

	override fun createGroup(name: String) {
		Timber.d("%s createGroup requested: name=%s", LOG_TAG, name)
		scope.launch {
			runCatching {
				api.syncPlayApi.syncPlayCreateGroup(
					data = org.jellyfin.sdk.model.api.NewGroupRequestDto(groupName = name)
				).content
			}.onSuccess { group ->
				Timber.d("%s createGroup success: groupName=%s groupId=%s", LOG_TAG, group.groupName, group.groupId)
				_state.update { it.copy(activeGroup = group, lastError = null) }
				joinGroup(group.groupId)
			}.onFailure { err ->
				Timber.w(err, "%s createGroup failed: name=%s", LOG_TAG, name)
				updateLastError(err.message ?: "SyncPlay create failed")
			}
		}
	}

	override fun joinGroup(groupId: UUID) {
		Timber.d("%s joinGroup requested: groupId=%s", LOG_TAG, groupId)
		scope.launch {
			runCatching {
				api.syncPlayApi.syncPlayJoinGroup(org.jellyfin.sdk.model.api.JoinGroupRequestDto(groupId))
			}.onSuccess {
				val joinedGroup = runCatching {
					api.syncPlayApi.syncPlayGetGroup(groupId).content
				}.getOrNull()

				if (joinedGroup != null) {
					_state.update { it.copy(activeGroup = joinedGroup, lastError = null) }
					Timber.d("%s joinGroup confirmed via API fallback: groupName=%s groupId=%s", LOG_TAG, joinedGroup.groupName, joinedGroup.groupId)
				} else {
					_state.update { it.copy(lastError = null) }
					Timber.d("%s joinGroup accepted, awaiting socket confirmation: groupId=%s", LOG_TAG, groupId)
				}

				refreshGroups()
			}.onFailure { err ->
				Timber.w(err, "%s joinGroup failed: groupId=%s", LOG_TAG, groupId)
				updateLastError(err.message ?: "SyncPlay join failed")
			}
		}
	}

	override fun leaveGroup() {
		Timber.d("%s leaveGroup requested", LOG_TAG)
		scope.launch {
			runCatching {
				api.syncPlayApi.syncPlayLeaveGroup()
			}.onSuccess {
				Timber.d("%s leaveGroup success", LOG_TAG)
				_state.update { it.copy(activeGroup = null, queueUpdate = null, stateUpdate = null, lastError = null) }
				refreshGroups()
			}.onFailure { err ->
				Timber.w(err, "%s leaveGroup failed", LOG_TAG)
				updateLastError(err.message ?: "SyncPlay leave failed")
			}
		}
	}

	override fun syncCurrentPlayback(
		itemIds: List<UUID>,
		playingIndex: Int,
		positionTicks: Long,
		isPlaying: Boolean,
	) {
		if (itemIds.isEmpty() || playingIndex !in itemIds.indices) {
			Timber.w(
				"%s syncCurrentPlayback rejected: itemCount=%s playingIndex=%s",
				LOG_TAG,
				itemIds.size,
				playingIndex,
			)
			updateLastError("No active playback to sync")
			return
		}

		Timber.d(
			"%s syncCurrentPlayback requested: itemCount=%s playingIndex=%s positionTicks=%s isPlaying=%s",
			LOG_TAG,
			itemIds.size,
			playingIndex,
			positionTicks,
			isPlaying,
		)

		pendingReady = PendingReady(
			itemId = itemIds[playingIndex],
			positionTicks = positionTicks,
			isPlaying = isPlaying,
		)

		scope.launch {
			runCatching {
				api.syncPlayApi.syncPlaySetNewQueue(
					PlayRequestDto(
						playingQueue = itemIds,
						playingItemPosition = playingIndex,
							startPositionTicks = positionTicks,
					)
				)
			}.onSuccess {
				Timber.d("%s syncCurrentPlayback queue sent", LOG_TAG)
			}.onFailure { err ->
				Timber.w(err, "%s syncCurrentPlayback failed to set queue", LOG_TAG)
				updateLastError(err.message ?: "SyncPlay queue update failed")
				pendingReady = null
			}
		}
	}

	override fun sendPause(positionTicks: Long) {
		if (!shouldSendLocalCommands()) {
			Timber.d("%s sendPause skipped: local commands suppressed or no active group", LOG_TAG)
			return
		}
		Timber.d("%s sendPause requested: positionTicks=%s", LOG_TAG, positionTicks)
		scope.launch {
			runCatching { api.syncPlayApi.syncPlayPause() }
				.onFailure { Timber.w(it, "%s sendPause failed", LOG_TAG) }
		}
	}

	override fun sendUnpause(positionTicks: Long) {
		if (!shouldSendLocalCommands()) {
			Timber.d("%s sendUnpause skipped: local commands suppressed or no active group", LOG_TAG)
			return
		}
		Timber.d("%s sendUnpause requested: positionTicks=%s", LOG_TAG, positionTicks)
		scope.launch {
			runCatching { api.syncPlayApi.syncPlayUnpause() }
				.onFailure { Timber.w(it, "%s sendUnpause failed", LOG_TAG) }
		}
	}

	override fun sendSeek(positionTicks: Long) {
		if (!shouldSendLocalCommands()) {
			Timber.d("%s sendSeek skipped: local commands suppressed or no active group", LOG_TAG)
			return
		}
		Timber.d("%s sendSeek requested: positionTicks=%s", LOG_TAG, positionTicks)
		scope.launch {
			runCatching {
				api.syncPlayApi.syncPlaySeek(SeekRequestDto(positionTicks = positionTicks))
			}.onFailure { Timber.w(it, "%s sendSeek failed", LOG_TAG) }
		}
	}

	override fun sendStop() {
		if (!shouldSendLocalCommands()) {
			Timber.d("%s sendStop skipped: local commands suppressed or no active group", LOG_TAG)
			return
		}
		Timber.d("%s sendStop requested", LOG_TAG)
		scope.launch {
			runCatching { api.syncPlayApi.syncPlayStop() }
				.onFailure { Timber.w(it, "%s sendStop failed", LOG_TAG) }
		}
	}

	override fun sendReady(playlistItemId: UUID, positionTicks: Long, isPlaying: Boolean) {
		Timber.d(
			"%s sendReady requested: playlistItemId=%s positionTicks=%s isPlaying=%s",
			LOG_TAG,
			playlistItemId,
			positionTicks,
			isPlaying,
		)
		scope.launch {
			runCatching {
				api.syncPlayApi.syncPlaySetPlaylistItem(SetPlaylistItemRequestDto(playlistItemId))
				api.syncPlayApi.syncPlayReady(
					ReadyRequestDto(
						`when` = LocalDateTime.now(),
						positionTicks = positionTicks,
						isPlaying = isPlaying,
						playlistItemId = playlistItemId,
					)
				)
			}.onSuccess {
				Timber.d("%s sendReady success: playlistItemId=%s", LOG_TAG, playlistItemId)
			}.onFailure { err ->
				Timber.w(err, "%s sendReady failed: playlistItemId=%s", LOG_TAG, playlistItemId)
				updateLastError(err.message ?: "SyncPlay ready failed")
			}
		}
	}

	override fun handleGroupUpdate(update: GroupUpdate) {
		Timber.d("%s handleGroupUpdate type=%s", LOG_TAG, update.type)
		when (update) {
			is SyncPlayGroupJoinedUpdate -> {
				_state.update { it.copy(activeGroup = update.data, lastError = null) }
			}
			is SyncPlayGroupLeftUpdate -> {
				_state.update { it.copy(activeGroup = null, queueUpdate = null, stateUpdate = null, lastError = null) }
			}
			is SyncPlayPlayQueueUpdate -> {
				_state.update { it.copy(queueUpdate = update.data, lastError = null) }
				handlePendingReady(update.data)
			}
			is org.jellyfin.sdk.model.api.SyncPlayStateUpdate -> {
				_state.update { it.copy(stateUpdate = update.data, lastError = null) }
			}
			is SyncPlayNotInGroupUpdate -> {
				Timber.w("%s server reported not in group: %s", LOG_TAG, update.data)
				_state.update { it.copy(activeGroup = null, queueUpdate = null, stateUpdate = null, lastError = update.data) }
			}
			is SyncPlayGroupDoesNotExistUpdate -> {
				Timber.w("%s server reported group does not exist: %s", LOG_TAG, update.data)
				_state.update { it.copy(activeGroup = null, queueUpdate = null, stateUpdate = null, lastError = update.data) }
			}
			is SyncPlayLibraryAccessDeniedUpdate -> {
				Timber.w("%s library access denied: %s", LOG_TAG, update.data)
				_state.update { it.copy(lastError = update.data) }
			}
			else -> {
				// Keep state as-is for other update types (user joined/left, etc.)
			}
		}

		if (update.type == GroupUpdateType.GROUP_JOINED || update.type == GroupUpdateType.GROUP_LEFT) {
			refreshGroups()
		}
	}

	override fun handleCommand(command: SendCommand) {
		if (command.command == SendCommandType.SEEK && command.positionTicks == null) {
			Timber.w("%s handleCommand dropped SEEK with null position", LOG_TAG)
			return
		}

		Timber.d(
			"%s handleCommand command=%s groupId=%s playlistItemId=%s when=%s emittedAt=%s positionTicks=%s",
			LOG_TAG,
			command.command,
			command.groupId,
			command.playlistItemId,
			command.`when`,
			command.emittedAt,
			command.positionTicks,
		)
	}

	override fun withRemoteCommand(block: () -> Unit) {
		Timber.v("%s withRemoteCommand enter", LOG_TAG)
		suppressLocalCommands.set(true)
		try {
			block()
		} finally {
			suppressLocalCommands.set(false)
			Timber.v("%s withRemoteCommand exit", LOG_TAG)
		}
	}

	private fun shouldSendLocalCommands(): Boolean {
		val active = _state.value.activeGroup != null
		return active && !suppressLocalCommands.get()
	}

	private fun updateLastError(message: String) {
		_state.update { it.copy(lastError = message) }
	}

	private fun handlePendingReady(queueUpdate: PlayQueueUpdate) {
		val pending = pendingReady ?: return
		Timber.d(
			"%s handlePendingReady evaluating: pendingItemId=%s queueSize=%s",
			LOG_TAG,
			pending.itemId,
			queueUpdate.playlist.size,
		)
		val playlistItemId = queueUpdate.playlist
			.firstOrNull { it.itemId == pending.itemId }
			?.playlistItemId
			?: run {
				Timber.d("%s handlePendingReady no match yet for pendingItemId=%s", LOG_TAG, pending.itemId)
				return
			}

		pendingReady = null
		scope.launch {
			runCatching {
				api.syncPlayApi.syncPlaySetPlaylistItem(SetPlaylistItemRequestDto(playlistItemId))
				api.syncPlayApi.syncPlayReady(
					ReadyRequestDto(
						`when` = LocalDateTime.now(),
						positionTicks = pending.positionTicks,
						isPlaying = pending.isPlaying,
						playlistItemId = playlistItemId,
					)
				)
			}.onSuccess {
				Timber.d(
					"%s handlePendingReady sent ready: playlistItemId=%s positionTicks=%s isPlaying=%s",
					LOG_TAG,
					playlistItemId,
					pending.positionTicks,
					pending.isPlaying,
				)
			}.onFailure { err ->
				Timber.w(err, "%s handlePendingReady failed: playlistItemId=%s", LOG_TAG, playlistItemId)
				updateLastError(err.message ?: "SyncPlay ready failed")
			}
		}
	}

	private data class PendingReady(
		val itemId: UUID,
		val positionTicks: Long,
		val isPlaying: Boolean,
	)
}
