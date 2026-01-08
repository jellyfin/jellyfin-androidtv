package org.jellyfin.androidtv.ui.playback

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleCoroutineScope
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.UserLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class PlaybackLauncherTests : FunSpec({
	test("playFromWatchNext launches playback with correct position") {
		// Arrange
		val mediaManager = mockk<MediaManager>(relaxed = true)
		val videoQueueManager = mockk<VideoQueueManager>(relaxed = true)
		val navigationRepository = mockk<NavigationRepository>(relaxed = true)
		val userPreferences = mockk<UserPreferences>(relaxed = true)
		val apiClient = mockk<ApiClient>(relaxed = true)
		val userLibraryApi = mockk<UserLibraryApi>(relaxed = true)
		
		val lifecycleOwner = mockk<LifecycleOwner>(relaxed = true)
		val lifecycleScope = mockk<LifecycleCoroutineScope>(relaxed = true)
		val context = mockk<Context>(relaxed = true)
		
		val itemId = UUID.randomUUID()
		val serverId = "test-server-id"
		val positionMs = 60000L // 1 minute
		
		val item = mockk<BaseItemDto>(relaxed = true) {
			every { id } returns itemId
			every { type } returns BaseItemKind.MOVIE
		}
		
		every { lifecycleOwner.lifecycleScope } returns lifecycleScope
		every { apiClient.userLibraryApi } returns userLibraryApi
		coEvery { userLibraryApi.getItem(itemId) } returns Response(item, 200, emptyMap())
		
		val launcher = PlaybackLauncher(
			mediaManager,
			videoQueueManager,
			navigationRepository,
			userPreferences,
			apiClient
		)
		
		// Act
		launcher.playFromWatchNext(lifecycleOwner, context, serverId, itemId.toString(), positionMs)
		
		// Assert
		coVerify { userLibraryApi.getItem(itemId) }
		verify { videoQueueManager.setCurrentVideoQueue(listOf(item)) }
	}
	
	test("playFromWatchNext handles ApiClientException gracefully") {
		// Arrange
		val mediaManager = mockk<MediaManager>(relaxed = true)
		val videoQueueManager = mockk<VideoQueueManager>(relaxed = true)
		val navigationRepository = mockk<NavigationRepository>(relaxed = true)
		val userPreferences = mockk<UserPreferences>(relaxed = true)
		val apiClient = mockk<ApiClient>(relaxed = true)
		val userLibraryApi = mockk<UserLibraryApi>(relaxed = true)
		
		val lifecycleOwner = mockk<LifecycleOwner>(relaxed = true)
		val lifecycleScope = mockk<LifecycleCoroutineScope>(relaxed = true)
		val context = mockk<Context>(relaxed = true)
		
		val itemId = UUID.randomUUID()
		val serverId = "test-server-id"
		val positionMs = 60000L
		
		every { lifecycleOwner.lifecycleScope } returns lifecycleScope
		every { apiClient.userLibraryApi } returns userLibraryApi
		coEvery { userLibraryApi.getItem(itemId) } throws ApiClientException("Network error")
		
		val launcher = PlaybackLauncher(
			mediaManager,
			videoQueueManager,
			navigationRepository,
			userPreferences,
			apiClient
		)
		
		// Act
		launcher.playFromWatchNext(lifecycleOwner, context, serverId, itemId.toString(), positionMs)
		
		// Assert - should not throw, should handle gracefully
		coVerify { userLibraryApi.getItem(itemId) }
		verify(exactly = 0) { videoQueueManager.setCurrentVideoQueue(any()) }
	}
	
	test("playFromWatchNext handles general exceptions gracefully") {
		// Arrange
		val mediaManager = mockk<MediaManager>(relaxed = true)
		val videoQueueManager = mockk<VideoQueueManager>(relaxed = true)
		val navigationRepository = mockk<NavigationRepository>(relaxed = true)
		val userPreferences = mockk<UserPreferences>(relaxed = true)
		val apiClient = mockk<ApiClient>(relaxed = true)
		val userLibraryApi = mockk<UserLibraryApi>(relaxed = true)
		
		val lifecycleOwner = mockk<LifecycleOwner>(relaxed = true)
		val lifecycleScope = mockk<LifecycleCoroutineScope>(relaxed = true)
		val context = mockk<Context>(relaxed = true)
		
		val itemId = UUID.randomUUID()
		val serverId = "test-server-id"
		val positionMs = 60000L
		
		every { lifecycleOwner.lifecycleScope } returns lifecycleScope
		every { apiClient.userLibraryApi } returns userLibraryApi
		coEvery { userLibraryApi.getItem(itemId) } throws RuntimeException("Unexpected error")
		
		val launcher = PlaybackLauncher(
			mediaManager,
			videoQueueManager,
			navigationRepository,
			userPreferences,
			apiClient
		)
		
		// Act
		launcher.playFromWatchNext(lifecycleOwner, context, serverId, itemId.toString(), positionMs)
		
		// Assert - should not throw, should handle gracefully
		coVerify { userLibraryApi.getItem(itemId) }
		verify(exactly = 0) { videoQueueManager.setCurrentVideoQueue(any()) }
	}
	
	test("playFromWatchNext converts position from milliseconds to integer correctly") {
		// Arrange
		val mediaManager = mockk<MediaManager>(relaxed = true)
		val videoQueueManager = mockk<VideoQueueManager>(relaxed = true)
		val navigationRepository = mockk<NavigationRepository>(relaxed = true)
		val userPreferences = mockk<UserPreferences>(relaxed = true)
		val apiClient = mockk<ApiClient>(relaxed = true)
		val userLibraryApi = mockk<UserLibraryApi>(relaxed = true)
		
		val lifecycleOwner = mockk<LifecycleOwner>(relaxed = true)
		val lifecycleScope = mockk<LifecycleCoroutineScope>(relaxed = true)
		val context = mockk<Context>(relaxed = true)
		
		val itemId = UUID.randomUUID()
		val serverId = "test-server-id"
		val positionMs = 123456L
		
		val item = mockk<BaseItemDto>(relaxed = true) {
			every { id } returns itemId
			every { type } returns BaseItemKind.EPISODE
		}
		
		every { lifecycleOwner.lifecycleScope } returns lifecycleScope
		every { apiClient.userLibraryApi } returns userLibraryApi
		coEvery { userLibraryApi.getItem(itemId) } returns Response(item, 200, emptyMap())
		
		val launcher = PlaybackLauncher(
			mediaManager,
			videoQueueManager,
			navigationRepository,
			userPreferences,
			apiClient
		)
		
		// Capture the launch parameters to verify position
		val contextSlot = slot<Context>()
		val itemsSlot = slot<List<BaseItemDto>>()
		val positionSlot = slot<Int>()
		every { 
			launcher.launch(
				capture(contextSlot), 
				capture(itemsSlot), 
				capture(positionSlot),
				any(),
				any(),
				any()
			)
		} returns Unit
		
		// Act
		launcher.playFromWatchNext(lifecycleOwner, context, serverId, itemId.toString(), positionMs)
		
		// Assert - position should be converted directly to Int
		coVerify { userLibraryApi.getItem(itemId) }
		verify { videoQueueManager.setCurrentVideoQueue(listOf(item)) }
		// Verify position is correctly converted from Long to Int
		assert(positionSlot.captured == positionMs.toInt())
	}
})
