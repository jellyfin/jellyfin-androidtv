package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.model.UserConfiguration
import org.jellyfin.androidtv.data.model.UserPolicy
import org.jellyfin.apiclient.discovery.DiscoveryServerInfo
import org.jellyfin.apiclient.model.apiclient.ServerInfo
import org.jellyfin.apiclient.model.dto.UserDto
import org.jellyfin.apiclient.model.system.PublicSystemInfo

fun DiscoveryServerInfo.toServerInfo(): ServerInfo = ServerInfo().apply {
	id = this@toServerInfo.id
	name = this@toServerInfo.name
	address = this@toServerInfo.address
}

fun DiscoveryServerInfo.toServer(): Server = Server(id, name, address)

fun PublicSystemInfo.toServer(): Server = Server(id, serverName, localAddress)

fun ServerInfo.toServer(): Server = Server(id, name, address, userId, accessToken, dateLastAccessed)

fun Server.toServerInfo(): ServerInfo = ServerInfo().apply {
	id = this@toServerInfo.id
	name = this@toServerInfo.name
	address = this@toServerInfo.address
	userId = this@toServerInfo.userId
	accessToken = this@toServerInfo.accessToken
	dateLastAccessed = this@toServerInfo.dateLastAccessed
}

fun UserDto.toUser(): User = User(
	id = id,
	name = name,
	serverId = serverId,
	primaryImageTag = primaryImageTag ?: "",
	hasPassword = hasPassword,
	hasConfiguredPassword = hasConfiguredPassword,
	hasConfiguredEasyPassword = hasConfiguredEasyPassword,
	configuration = UserConfiguration(
		latestItemsExcludes = configuration.latestItemsExcludes
	),
	policy = UserPolicy(
		enableLiveTvAccess = policy.enableLiveTvAccess,
		enableLiveTvManagement = policy.enableLiveTvManagement
	)
)

fun User.toUserDto(): UserDto = UserDto().apply {
	id = this@toUserDto.id
	name = this@toUserDto.name
	serverId = this@toUserDto.serverId
	primaryImageTag = this@toUserDto.primaryImageTag
	hasPassword = this@toUserDto.hasPassword
	hasConfiguredPassword = this@toUserDto.hasConfiguredPassword
	hasConfiguredEasyPassword = this@toUserDto.hasConfiguredEasyPassword
	configuration = org.jellyfin.apiclient.model.configuration.UserConfiguration().apply {
		latestItemsExcludes = this@toUserDto.configuration.latestItemsExcludes
	}
	policy = org.jellyfin.apiclient.model.users.UserPolicy().apply {
		enableLiveTvAccess = this@toUserDto.policy.enableLiveTvAccess
		enableLiveTvManagement = this@toUserDto.policy.enableLiveTvManagement
	}
}
