package org.jellyfin.androidtv.ui.playback

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayClient
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayRequest
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayStatus
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.GroupInfoDto
import org.jellyfin.sdk.model.api.GroupStateType
import java.time.LocalDateTime
import java.util.UUID

class PlaybackLauncherSyncPlayTests : FunSpec({
	test("launch in a group submits authoritative queue instead of navigating locally") {
		runTest {
			val firstId = UUID.randomUUID()
			val secondId = UUID.randomUUID()
			val items = listOf(item(firstId), item(secondId))
			val syncPlay = RecordingSyncPlayClient(joined = true)
			val navigation = mockk<NavigationRepository>(relaxed = true)
			val videoQueue = mockk<VideoQueueManager>(relaxed = true)
			val launcher = PlaybackLauncher(
				mediaManager = mockk(relaxed = true),
				videoQueueManager = videoQueue,
				navigationRepository = navigation,
				userPreferences = mockk<UserPreferences>(relaxed = true),
				syncPlay = syncPlay,
				coroutineScope = this,
			)

			launcher.launch(mockk<Context>(relaxed = true), items, position = 1234, itemsPosition = 1)
			advanceUntilIdle()

			val request = syncPlay.requests.single() as SyncPlayRequest.SetQueue
			request.data.playingQueue shouldBe listOf(firstId, secondId)
			request.data.playingItemPosition shouldBe 1
			request.data.startPositionTicks shouldBe 12_340_000L
			verify(exactly = 0) { navigation.navigate(any(), any()) }
			verify(exactly = 0) { videoQueue.setCurrentVideoQueue(any()) }
		}
	}
})

private fun item(id: UUID): BaseItemDto {
	val item = mockk<BaseItemDto>()
	every { item.id } returns id
	return item
}

private class RecordingSyncPlayClient(joined: Boolean) : SyncPlayClient {
	override val state = MutableStateFlow(
		SyncPlayStatus(
			group = if (joined) GroupInfoDto(
				UUID.randomUUID(), "Group", GroupStateType.IDLE, emptyList(), LocalDateTime.MIN
			) else null,
		)
	)
	val requests = mutableListOf<SyncPlayRequest>()

	override suspend fun groups() = emptyList<GroupInfoDto>()
	override suspend fun create(name: String) = false
	override suspend fun join(id: UUID) = false
	override suspend fun leave() = Unit
	override suspend fun halt() = Unit
	override suspend fun resume() = Unit
	override suspend fun request(request: SyncPlayRequest): Boolean { requests += request; return true }
	override fun close() = Unit
}
