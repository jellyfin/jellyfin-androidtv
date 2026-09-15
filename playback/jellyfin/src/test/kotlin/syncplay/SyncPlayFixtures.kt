package org.jellyfin.playback.jellyfin.syncplay

import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupRepeatMode
import org.jellyfin.sdk.model.api.GroupShuffleMode
import org.jellyfin.sdk.model.api.GroupStateType
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import org.jellyfin.sdk.model.api.PlayQueueUpdateReason
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SendCommandType
import org.jellyfin.sdk.model.api.SyncPlayQueueItem
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

internal val groupId = UUID(0, 1)
internal val otherGroupId = UUID(0, 2)
internal val itemId = UUID(0, 3)
internal val occurrenceId = UUID(0, 4)
internal fun date(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
internal fun group(id: UUID = groupId) = GroupInfoDto(id, "Watch together", GroupStateType.PAUSED, listOf("Alex"), date(1000))
internal fun queue(updated: Long = 1100, index: Int = 0) = PlayQueueUpdate(
	reason = PlayQueueUpdateReason.NEW_PLAYLIST,
	lastUpdate = date(updated),
	playlist = listOf(SyncPlayQueueItem(itemId, occurrenceId), SyncPlayQueueItem(itemId, UUID(0, 5))),
	playingItemIndex = index,
	startPositionTicks = 0,
	isPlaying = false,
	shuffleMode = GroupShuffleMode.SORTED,
	repeatMode = GroupRepeatMode.REPEAT_NONE,
)
@Suppress("LongParameterList")
internal fun command(
	type: SendCommandType = SendCommandType.UNPAUSE,
	emitted: Long = 1200,
	whenMillis: Long = 2000,
	position: Long? = 0,
	group: UUID = groupId,
	occurrence: UUID = occurrenceId,
) = SendCommand(group, occurrence, date(whenMillis), position, type, date(emitted))
