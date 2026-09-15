package org.jellyfin.playback.jellyfin.syncplay

import kotlinx.coroutines.flow.mapNotNull
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.syncPlayApi
import org.jellyfin.sdk.api.client.extensions.timeSyncApi
import org.jellyfin.sdk.api.sockets.subscribe
import org.jellyfin.sdk.model.api.BufferRequestDto
import org.jellyfin.sdk.model.api.IgnoreWaitRequestDto
import org.jellyfin.sdk.model.api.JoinGroupRequestDto
import org.jellyfin.sdk.model.api.NewGroupRequestDto
import org.jellyfin.sdk.model.api.NextItemRequestDto
import org.jellyfin.sdk.model.api.OutboundWebSocketMessage
import org.jellyfin.sdk.model.api.PingRequestDto
import org.jellyfin.sdk.model.api.PreviousItemRequestDto
import org.jellyfin.sdk.model.api.ReadyRequestDto
import org.jellyfin.sdk.model.api.SeekRequestDto
import org.jellyfin.sdk.model.api.SetPlaylistItemRequestDto
import org.jellyfin.sdk.model.api.SyncPlayCommandMessage
import org.jellyfin.sdk.model.api.SyncPlayGroupUpdateMessage
import java.util.UUID

class SdkSyncPlayTransport(private val api: ApiClient) : SyncPlayTransport {
	// One stream preserves ordering between group/queue updates and playback commands.
	// The base message subscription avoids subscribeAll(), which starts unrelated server feeds.
	// Combining separate subscriptions could reorder these messages. The SDK owns the connection.
	override val events get() = api.webSocket.subscribe<OutboundWebSocketMessage>().mapNotNull { message ->
		when (message) {
			is SyncPlayCommandMessage -> message.data?.let(SyncPlayEvent::Command)
			is SyncPlayGroupUpdateMessage -> SyncPlayEvent.GroupUpdate(message.data)
			else -> null
		}
	}
	override val connectionState get() = api.webSocket.state

	override suspend fun groups() = api.syncPlayApi.syncPlayGetGroups().content
	override suspend fun createGroup(name: String) = api.syncPlayApi.syncPlayCreateGroup(NewGroupRequestDto(name)).content
	override suspend fun joinGroup(id: UUID) {
		api.syncPlayApi.syncPlayJoinGroup(JoinGroupRequestDto(id))
	}
	override suspend fun leaveGroup() {
		api.syncPlayApi.syncPlayLeaveGroup()
	}
	override suspend fun time() = api.timeSyncApi.getUtcTime().content

	@Suppress("CyclomaticComplexMethod") // Exhaustive mapping of the protocol request variants.
	override suspend fun send(request: SyncPlayRequest) {
		val syncPlay = api.syncPlayApi
		when (request) {
			SyncPlayRequest.Pause -> syncPlay.syncPlayPause()
			SyncPlayRequest.Unpause -> syncPlay.syncPlayUnpause()
			SyncPlayRequest.Stop -> syncPlay.syncPlayStop()
			is SyncPlayRequest.Seek -> syncPlay.syncPlaySeek(SeekRequestDto(request.positionTicks))
			is SyncPlayRequest.Next -> syncPlay.syncPlayNextItem(NextItemRequestDto(request.playlistItemId))
			is SyncPlayRequest.Previous -> syncPlay.syncPlayPreviousItem(PreviousItemRequestDto(request.playlistItemId))
			is SyncPlayRequest.Select -> syncPlay.syncPlaySetPlaylistItem(SetPlaylistItemRequestDto(request.playlistItemId))
			is SyncPlayRequest.IgnoreWait -> syncPlay.syncPlaySetIgnoreWait(IgnoreWaitRequestDto(request.ignore))
			is SyncPlayRequest.Ping -> syncPlay.syncPlayPing(PingRequestDto(request.milliseconds))
			is SyncPlayRequest.SetQueue -> syncPlay.syncPlaySetNewQueue(request.data)
			is SyncPlayRequest.Queue -> syncPlay.syncPlayQueue(request.data)
			is SyncPlayRequest.Remove -> syncPlay.syncPlayRemoveFromPlaylist(request.data)
			is SyncPlayRequest.Move -> syncPlay.syncPlayMovePlaylistItem(request.data)
			is SyncPlayRequest.Repeat -> syncPlay.syncPlaySetRepeatMode(request.data)
			is SyncPlayRequest.Shuffle -> syncPlay.syncPlaySetShuffleMode(request.data)
			is SyncPlayRequest.Ready -> syncPlay.syncPlayReady(
				ReadyRequestDto(request.whenTime, request.positionTicks, request.isPlaying, request.playlistItemId)
			)
			is SyncPlayRequest.Buffering -> syncPlay.syncPlayBuffering(
				BufferRequestDto(request.whenTime, request.positionTicks, request.isPlaying, request.playlistItemId)
			)
		}
	}
}
