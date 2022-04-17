package org.jellyfin.androidtv.util.sdk

import org.jellyfin.androidtv.auth.model.PublicUser
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.sdk.model.api.NameGuidPair
import org.jellyfin.sdk.model.api.NameIdPair
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo
import org.jellyfin.sdk.model.api.UserDto
import org.jellyfin.sdk.model.serializer.toUUID
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

fun ServerDiscoveryInfo.toServer(): Server = Server(
	id = id.toUUID(),
	name = name,
	address = address,
)

fun UserDto.toPublicUser(): PublicUser? {
	return PublicUser(
		id = id,
		name = name ?: return null,
		serverId = serverId?.toUUIDOrNull() ?: return null,
		accessToken = null,
		requirePassword = hasPassword,
		imageTag = primaryImageTag
	)
}


fun NameIdPair.toNameGuidPair(): NameGuidPair = NameGuidPair(
	id = this.id!!.toUUID(),
	name = this.name
)
