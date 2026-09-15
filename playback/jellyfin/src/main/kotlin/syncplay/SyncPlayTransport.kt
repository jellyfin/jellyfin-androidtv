package org.jellyfin.playback.jellyfin.syncplay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.sdk.api.sockets.SocketApiState
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.MovePlaylistItemRequestDto
import org.jellyfin.sdk.model.api.PlayRequestDto
import org.jellyfin.sdk.model.api.QueueRequestDto
import org.jellyfin.sdk.model.api.RemoveFromPlaylistRequestDto
import org.jellyfin.sdk.model.api.SendCommand
import org.jellyfin.sdk.model.api.SetRepeatModeRequestDto
import org.jellyfin.sdk.model.api.SetShuffleModeRequestDto
import org.jellyfin.sdk.model.api.UtcTimeResponse
import java.time.LocalDateTime
import java.util.UUID
import org.jellyfin.sdk.model.api.GroupUpdate as SdkGroupUpdate

sealed interface SyncPlayEvent {
	data class Command(val data: SendCommand) : SyncPlayEvent
	data class GroupUpdate(val data: SdkGroupUpdate) : SyncPlayEvent
}

/** User/group requests are distinct from raw player actions, preventing command echo loops. */
sealed interface SyncPlayRequest {
	data object Pause : SyncPlayRequest
	data object Unpause : SyncPlayRequest
	data object Stop : SyncPlayRequest
	data class Seek(val positionTicks: Long) : SyncPlayRequest
	data class Next(val playlistItemId: UUID) : SyncPlayRequest
	data class Previous(val playlistItemId: UUID) : SyncPlayRequest
	data class Select(val playlistItemId: UUID) : SyncPlayRequest
	data class IgnoreWait(val ignore: Boolean) : SyncPlayRequest
	data class Ping(val milliseconds: Long) : SyncPlayRequest
	data class SetQueue(val data: PlayRequestDto) : SyncPlayRequest
	data class Queue(val data: QueueRequestDto) : SyncPlayRequest
	data class Remove(val data: RemoveFromPlaylistRequestDto) : SyncPlayRequest
	data class Move(val data: MovePlaylistItemRequestDto) : SyncPlayRequest
	data class Repeat(val data: SetRepeatModeRequestDto) : SyncPlayRequest
	data class Shuffle(val data: SetShuffleModeRequestDto) : SyncPlayRequest
	data class Ready(
		val whenTime: LocalDateTime,
		val positionTicks: Long,
		val isPlaying: Boolean,
		val playlistItemId: UUID,
	) : SyncPlayRequest
	data class Buffering(
		val whenTime: LocalDateTime,
		val positionTicks: Long,
		val isPlaying: Boolean,
		val playlistItemId: UUID,
	) : SyncPlayRequest
}

/** Bound to one authenticated ApiClient; no automatic retries of ambiguous mutations. */
interface SyncPlayTransport {
	val events: Flow<SyncPlayEvent>
	val connectionState: StateFlow<SocketApiState>
	suspend fun groups(): List<GroupInfoDto>
	suspend fun createGroup(name: String): GroupInfoDto
	suspend fun joinGroup(id: UUID)
	suspend fun leaveGroup()
	suspend fun time(): UtcTimeResponse
	suspend fun send(request: SyncPlayRequest)
}
