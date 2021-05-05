package org.jellyfin.androidtv.util.sdk

import org.jellyfin.androidtv.auth.model.PublicUser
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.sdk.model.ServerVersion
import org.jellyfin.sdk.model.api.ServerDiscoveryInfo
import org.jellyfin.sdk.model.api.UserDto
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

fun ServerDiscoveryInfo.toServer(): Server? {
	return Server(
		id = id?.toUUIDOrNull() ?: return null,
		name = name ?: return null,
		address = address ?: return null,
	)
}

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

@Deprecated(
	"Moved to SDK. See https://github.com/jellyfin/jellyfin-sdk-kotlin/pull/269",
	ReplaceWith("toString()"),
	DeprecationLevel.WARNING
)
fun ServerVersion.asText(): String = buildString {
	append(major, '.', minor, '.', patch)
	if (build != null) append('.', build)
}
