package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.util.toUUID
import org.jellyfin.apiclient.discovery.DiscoveryServerInfo
import org.jellyfin.apiclient.model.dto.UserDto

fun DiscoveryServerInfo.toServer() = Server(id.toUUID(), name, address)

fun UserDto.toUser() = User(
	id = id.toUUID(),
	name = name,
	serverId = serverId.toUUID(),
	accessToken = null
)
