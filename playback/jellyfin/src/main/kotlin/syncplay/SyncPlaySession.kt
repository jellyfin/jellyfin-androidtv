package org.jellyfin.playback.jellyfin.syncplay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import java.util.UUID

/** Snapshot of server-owned state. A group may be joined without an attached player. */
data class SyncPlaySessionState(
	val generation: Long = 0,
	val joining: Boolean = false,
	val group: GroupInfoDto? = null,
	val queue: PlayQueueUpdate? = null,
	val command: SendCommand? = null,
) {
	val currentItem get() = queue?.let { it.playlist.getOrNull(it.playingItemIndex) }

	// Retain commands that arrive before their queue, but never apply them to another occurrence.
	val playbackCommand: SendCommand?
		get() = command?.takeIf { it.command == SendCommandType.STOP || it.playlistItemId == currentItem?.playlistItemId }
}

/**
 * Session reducer. The caller serializes access and passes the generation captured BEFORE each
 * asynchronous operation. Reset on disconnect, leave or account changes before accepting more work.
 */
class SyncPlaySession {
	private val mutableState = MutableStateFlow(SyncPlaySessionState())
	val state = mutableState.asStateFlow()
	private var expectedGroupId: UUID? = null

	/** A null group ID means Create; a non-null ID means Join. */
	fun beginJoin(groupId: UUID? = null): Long {
		reset()
		expectedGroupId = groupId
		mutableState.value = mutableState.value.copy(joining = true)
		return mutableState.value.generation
	}

	fun joined(generation: Long, group: GroupInfoDto): Boolean {
		val current = mutableState.value
		if (generation != current.generation) return false
		if (current.group != null) return current.group.groupId == group.groupId
		if (!current.joining || (expectedGroupId != null && expectedGroupId != group.groupId)) return false
		mutableState.value = current.copy(joining = false, group = group.copy(participants = group.participants.toList()))
		return true
	}

	fun updateQueue(generation: Long, groupId: UUID, queue: PlayQueueUpdate): Boolean {
		val current = mutableState.value
		if (!owns(generation, groupId)) return false
		if (queue.playingItemIndex < -1 || queue.playingItemIndex >= queue.playlist.size) return false
		if (queue.startPositionTicks < 0) return false
		if (queue.playlist.map { it.playlistItemId }.distinct().size != queue.playlist.size) return false
		if (current.queue?.let { queue.lastUpdate <= it.lastUpdate } == true) return false
		mutableState.value = current.copy(queue = queue.copy(playlist = queue.playlist.toList()))
		return true
	}

	fun acceptCommand(generation: Long, command: SendCommand): Boolean {
		val current = mutableState.value
		if (!owns(generation, command.groupId)) return false
		if (command.emittedAt < requireNotNull(current.group).lastUpdatedAt) return false
		if (current.command?.let { command.emittedAt < it.emittedAt } == true) return false
		if (command.command != SendCommandType.STOP && (command.positionTicks?.let { it >= 0 } != true)) return false
		mutableState.value = current.copy(command = command)
		return true
	}

	fun reset() {
		expectedGroupId = null
		mutableState.value = SyncPlaySessionState(generation = mutableState.value.generation + 1)
	}

	private fun owns(generation: Long, groupId: UUID) =
		generation == mutableState.value.generation && mutableState.value.group?.groupId == groupId
}
