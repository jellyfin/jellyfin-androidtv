package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.apiclient.discovery.DiscoveryServerInfo
import org.jellyfin.apiclient.model.dto.UserDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class ModelExtensionsTest {
	private companion object {
		private val SERVER_ID = UUID.fromString("18c5136b-296f-4664-bbd3-a8247b4cc20c")
		private const val SERVER_URL = "https://example.com"
		private const val SERVER_NAME = "Test Server"
		private val USER_ID = UUID.fromString("5331276d-d620-44c7-af8f-53eb4ff56f7f")
		private const val USER_NAME = "User Name"
		private const val IMAGE_TAG = "tag"
		private val EXCLUDE_ITEMS = listOf("exclude")
	}

	@Test
	fun discoverServerInfoToServer() {
		val server = DiscoveryServerInfo(SERVER_ID.toString(), SERVER_URL, SERVER_NAME, "").toServer()
		assertEquals(SERVER_ID, server.id)
		assertEquals(SERVER_URL, server.address)
		assertEquals(SERVER_NAME, server.name)
	}

	@Test
	fun userDtoToUser() {
		val user = UserDto().apply {
			id = USER_ID.toString()
			name = USER_NAME
			serverId = SERVER_ID.toString()
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
	}
}
