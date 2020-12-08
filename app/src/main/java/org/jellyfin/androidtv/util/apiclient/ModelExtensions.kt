package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.util.toUUID
import org.jellyfin.apiclient.discovery.DiscoveryServerInfo
import org.jellyfin.apiclient.model.apiclient.ServerInfo
import org.jellyfin.apiclient.model.dto.UserDto
import org.jellyfin.apiclient.model.system.PublicSystemInfo

fun DiscoveryServerInfo.toServerInfo() = ServerInfo().apply {
	id = this@toServerInfo.id
	name = this@toServerInfo.name
	address = this@toServerInfo.address
}

fun DiscoveryServerInfo.toServer() = Server(id.toUUID(), name, address)

fun PublicSystemInfo.toServer() = Server(id.toUUID(), serverName, localAddress)

fun ServerInfo.toServer() = Server(id.toUUID(), name, address, dateLastAccessed)

fun Server.toServerInfo() = ServerInfo().also { server ->
	id = server.id.toUUID()
	name = server.name
	address = server.address
	dateLastAccessed = server.dateLastAccessed
}

fun UserDto.toUser() = User(
	id = id.toUUID(),
	name = name,
	serverId = serverId.toUUID(),
	accessToken = null
)
