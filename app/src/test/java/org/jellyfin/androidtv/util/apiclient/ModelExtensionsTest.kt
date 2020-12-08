package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.apiclient.discovery.DiscoveryServerInfo
import org.jellyfin.apiclient.model.dto.UserDto
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelExtensionsTest {
	private companion object {
		private const val SERVER_ID = "id"
		private const val SERVER_URL = "https://example.com"
		private const val SERVER_NAME = "Test Server"
		private const val USER_ID = "userId"
		private const val USER_NAME = "User Name"
		private const val IMAGE_TAG = "tag"
		private val EXCLUDE_ITEMS = listOf("exclude")
	}

	@Test
	fun discoverServerInfoToServer() {
		val server = DiscoveryServerInfo(SERVER_ID, SERVER_URL, SERVER_NAME, "").toServer()
		assertEquals(SERVER_ID, server.id)
		assertEquals(SERVER_URL, server.address)
		assertEquals(SERVER_NAME, server.name)
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
	}
}
