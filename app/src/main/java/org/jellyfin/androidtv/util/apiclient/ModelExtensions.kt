package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.data.model.LegacyServer
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

fun DiscoveryServerInfo.toServer() = Server(id, name, address)

fun PublicSystemInfo.toServer() = Server(id, serverName, localAddress)

fun ServerInfo.toLegacyServer() = LegacyServer(id, name, address, dateLastAccessed, userId, accessToken)

fun ServerInfo.toServer() = Server(id, name, address, dateLastAccessed)

fun Server.toServerInfo() = ServerInfo().apply {
	id = this@toServerInfo.id
	name = this@toServerInfo.name
	address = this@toServerInfo.address
	dateLastAccessed = this@toServerInfo.dateLastAccessed
}

fun LegacyServer.toServerInfo() = ServerInfo().apply {
	id = this@toServerInfo.id
	name = this@toServerInfo.name
	address = this@toServerInfo.address
	userId = this@toServerInfo.userId
	accessToken = this@toServerInfo.accessToken
	dateLastAccessed = this@toServerInfo.dateLastAccessed
}

fun UserDto.toUser() = User(
	id = id.toUUID(),
	name = name,
	serverId = serverId.toUUID(),
	accessToken = null
)
