package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.data.model.LegacyServer
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.model.UserConfiguration
import org.jellyfin.androidtv.data.model.UserPolicy
import org.jellyfin.apiclient.discovery.DiscoveryServerInfo
import org.jellyfin.apiclient.model.apiclient.ServerInfo
import org.jellyfin.apiclient.model.dto.UserDto
import org.jellyfin.apiclient.model.system.PublicSystemInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

class ModelExtensionsTest {
	private companion object {
		private const val SERVER_ID = "id"
		private const val SERVER_URL = "https://example.com"
		private const val SERVER_NAME = "Test Server"
		private const val USER_ID = "userId"
		private const val USER_NAME = "User Name"
		private const val ACCESS_TOKEN = "token"
		private const val IMAGE_TAG = "tag"
		private val EXCLUDE_ITEMS = listOf("exclude")
	}

	@Test
	fun discoveryServerInfoToServerInfo() {
		val serverInfo = DiscoveryServerInfo(SERVER_ID, SERVER_URL, SERVER_NAME, "").toServerInfo()
		assertEquals(SERVER_ID, serverInfo.id)
		assertEquals(SERVER_URL, serverInfo.address)
		assertEquals(SERVER_NAME, serverInfo.name)
	}

	@Test
	fun discoverServerInfoToServer() {
		val server = DiscoveryServerInfo(SERVER_ID, SERVER_URL, SERVER_NAME, "").toServer()
		assertEquals(SERVER_ID, server.id)
		assertEquals(SERVER_URL, server.address)
		assertEquals(SERVER_NAME, server.name)
	}

	@Test
	fun publicSystemInfoToServer() {
		val server = PublicSystemInfo().apply {
			id = SERVER_ID
			serverName = SERVER_NAME
			localAddress = SERVER_URL
		}.toServer()
		assertEquals(SERVER_ID, server.id)
		assertEquals(SERVER_URL, server.address)
		assertEquals(SERVER_NAME, server.name)
	}

	@Test
	fun serverInfoToServer() {
		val server = ServerInfo().apply {
			id = SERVER_ID
			address = SERVER_URL
			name = SERVER_NAME
		}.toServer()
		assertEquals(SERVER_ID, server.id)
		assertEquals(SERVER_URL, server.address)
		assertEquals(SERVER_NAME, server.name)
	}

	@Test
	fun serverInfoToLegacyServer() {
		val server = ServerInfo().apply {
			id = SERVER_ID
			address = SERVER_URL
			name = SERVER_NAME
			userId = USER_ID
			accessToken = ACCESS_TOKEN
		}.toLegacyServer()
		assertEquals(SERVER_ID, server.id)
		assertEquals(SERVER_URL, server.address)
		assertEquals(SERVER_NAME, server.name)
		assertEquals(USER_ID, server.userId)
		assertEquals(ACCESS_TOKEN, server.accessToken)
	}

	@Test
	fun serverToServerInfo() {
		val instant = LocalDate.of(2020, 10, 1)
			.atStartOfDay(ZoneOffset.UTC)
			.toInstant()
		val serverInfo = Server(
			SERVER_ID,
			SERVER_NAME,
			SERVER_URL,
			Date.from(instant)
		).toServerInfo()
		assertEquals(SERVER_ID, serverInfo.id)
		assertEquals(SERVER_NAME, serverInfo.name)
		assertEquals(SERVER_URL, serverInfo.address)
		assertEquals(instant, serverInfo.dateLastAccessed.toInstant())
	}
	@Test
	fun legacyServerToServerInfo() {
		val instant = LocalDate.of(2020, 10, 1)
			.atStartOfDay(ZoneOffset.UTC)
			.toInstant()
		val serverInfo = LegacyServer(
			SERVER_ID,
			SERVER_NAME,
			SERVER_URL,
			Date.from(instant),
			USER_ID,
			ACCESS_TOKEN
		).toServerInfo()
		assertEquals(SERVER_ID, serverInfo.id)
		assertEquals(SERVER_NAME, serverInfo.name)
		assertEquals(SERVER_URL, serverInfo.address)
		assertEquals(USER_ID, serverInfo.userId)
		assertEquals(ACCESS_TOKEN, serverInfo.accessToken)
		assertEquals(instant, serverInfo.dateLastAccessed.toInstant())
	}

	@Test
	fun userDtoToUser() {
		val user = UserDto().apply {
			id = USER_ID
			name = USER_NAME
			serverId = SERVER_ID
			primaryImageTag = IMAGE_TAG
			hasPassword = true
			hasConfiguredPassword = true
			hasConfiguredEasyPassword = false
			policy = org.jellyfin.apiclient.model.users.UserPolicy().apply {
				enableLiveTvAccess = false
				enableLiveTvManagement = false
			}
			configuration = org.jellyfin.apiclient.model.configuration.UserConfiguration().apply {
				latestItemsExcludes = EXCLUDE_ITEMS.toTypedArray()
			}
		}.toUser()
		assertEquals(USER_ID, user.id)
		assertEquals(USER_NAME, user.name)
		assertEquals(SERVER_ID, user.serverId)
		assertEquals(IMAGE_TAG, user.primaryImageTag)
		assertTrue(user.hasPassword)
		assertTrue(user.hasConfiguredPassword)
		assertFalse(user.hasConfiguredEasyPassword)
		assertFalse(user.policy.enableLiveTvAccess)
		assertFalse(user.policy.enableLiveTvManagement)
		assertTrue(EXCLUDE_ITEMS == user.configuration.latestItemsExcludes)
	}

	@Test
	fun userToUserDto() {
		val userDto = User(
			id = USER_ID,
			name = USER_NAME,
			serverId = SERVER_ID,
			primaryImageTag = IMAGE_TAG,
			hasPassword = true,
			hasConfiguredPassword = true,
			hasConfiguredEasyPassword = false,
			configuration = UserConfiguration(
				latestItemsExcludes = EXCLUDE_ITEMS
			),
			policy = UserPolicy(
				enableLiveTvAccess = false,
				enableLiveTvManagement = false
			)
		).toUserDto()
		assertEquals(USER_ID, userDto.id)
		assertEquals(USER_NAME, userDto.name)
		assertEquals(SERVER_ID, userDto.serverId)
		assertEquals(IMAGE_TAG, userDto.primaryImageTag)
		assertTrue(userDto.hasPassword)
		assertTrue(userDto.hasConfiguredPassword)
		assertFalse(userDto.hasConfiguredEasyPassword)
		assertTrue(EXCLUDE_ITEMS == userDto.configuration.latestItemsExcludes.asList())
		assertFalse(userDto.policy.enableLiveTvAccess)
		assertFalse(userDto.policy.enableLiveTvManagement)
	}
}
