package org.jellyfin.playback.jellyfin.syncplay

import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.sdk.model.api.GroupInfoDto
import java.util.UUID

/** Membership operations exposed to application UI. */
interface SyncPlayClient {
	val state: StateFlow<SyncPlayStatus>

	suspend fun groups(): List<GroupInfoDto>
	suspend fun create(name: String): Boolean
	suspend fun join(id: UUID): Boolean
	suspend fun leave()
	suspend fun halt()
	suspend fun resume()
	suspend fun request(request: SyncPlayRequest): Boolean
	fun close()
}
