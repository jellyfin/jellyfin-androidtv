package syncplay

import androidx.lifecycle.Lifecycle
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.lang.reflect.Method
import java.time.LocalDateTime
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.syncplay.SyncPlayRepository
import org.jellyfin.androidtv.syncplay.SyncPlaySocketHandler
import org.jellyfin.androidtv.syncplay.SyncPlayLoopGuard
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.GroupRepeatMode
import org.jellyfin.sdk.model.api.GroupShuffleMode
import org.jellyfin.sdk.model.api.PlayQueueUpdate
import org.jellyfin.sdk.model.api.PlayQueueUpdateReason
import org.jellyfin.sdk.model.api.SyncPlayQueueItem
import org.jellyfin.sdk.model.serializer.toUUID

class SyncPlaySocketHandlerTests : FunSpec({
	test("sendReady is deduped for identical queue state") {
		val repository = mockk<SyncPlayRepository>(relaxed = true)
		val handler = SyncPlaySocketHandler(
			api = mockk<ApiClient>(relaxed = true),
			repository = repository,
			playbackControllerContainer = mockk<PlaybackControllerContainer>(relaxed = true),
			videoQueueManager = mockk<VideoQueueManager>(relaxed = true),
			navigationRepository = mockk<NavigationRepository>(relaxed = true),
			userPreferences = mockk<UserPreferences>(relaxed = true),
			lifecycle = mockk<Lifecycle>(relaxed = true),
			loopGuard = SyncPlayLoopGuard(nowMs = { 1_000L }),
			startListening = false,
		)

		val playlistItemId = "689eb948-a547-4bc0-8abd-8cc2ce2cc647".toUUID()
		val itemId = "d45bcd06-495a-0851-ae8f-5393892d9a4a".toUUID()
		val queue = PlayQueueUpdate(
			reason = PlayQueueUpdateReason.SET_CURRENT_ITEM,
			lastUpdate = LocalDateTime.now(),
			playlist = listOf(SyncPlayQueueItem(itemId = itemId, playlistItemId = playlistItemId)),
			playingItemIndex = 0,
			startPositionTicks = 0,
			isPlaying = false,
			shuffleMode = GroupShuffleMode.SORTED,
			repeatMode = GroupRepeatMode.REPEAT_NONE,
		)

		val sendReady = handler.javaClass.getDeclaredMethod(
			"sendReady",
			PlayQueueUpdate::class.java,
			Int::class.javaPrimitiveType,
		)
		sendReady.isAccessible = true

		sendReady.invoke(handler, queue, 0)
		sendReady.invoke(handler, queue, 0)

		verify(exactly = 1) { repository.sendReady(playlistItemId, 0, false) }
	}

	test("sendReady forwards when queue state changes") {
		val repository = mockk<SyncPlayRepository>(relaxed = true)
		val handler = SyncPlaySocketHandler(
			api = mockk<ApiClient>(relaxed = true),
			repository = repository,
			playbackControllerContainer = mockk<PlaybackControllerContainer>(relaxed = true),
			videoQueueManager = mockk<VideoQueueManager>(relaxed = true),
			navigationRepository = mockk<NavigationRepository>(relaxed = true),
			userPreferences = mockk<UserPreferences>(relaxed = true),
			lifecycle = mockk<Lifecycle>(relaxed = true),
			loopGuard = SyncPlayLoopGuard(nowMs = { 2_000L }),
			startListening = false,
		)

		val playlistItemId = "689eb948-a547-4bc0-8abd-8cc2ce2cc647".toUUID()
		val itemId = "d45bcd06-495a-0851-ae8f-5393892d9a4a".toUUID()
		val queueA = PlayQueueUpdate(
			reason = PlayQueueUpdateReason.SET_CURRENT_ITEM,
			lastUpdate = LocalDateTime.now(),
			playlist = listOf(SyncPlayQueueItem(itemId = itemId, playlistItemId = playlistItemId)),
			playingItemIndex = 0,
			startPositionTicks = 0,
			isPlaying = false,
			shuffleMode = GroupShuffleMode.SORTED,
			repeatMode = GroupRepeatMode.REPEAT_NONE,
		)
		val queueB = queueA.copy(startPositionTicks = 10_000)

		val sendReady = handler.javaClass.getDeclaredMethod(
			"sendReady",
			PlayQueueUpdate::class.java,
			Int::class.javaPrimitiveType,
		)
		sendReady.isAccessible = true

		sendReady.invoke(handler, queueA, 0)
		sendReady.invoke(handler, queueB, 0)

		verify(exactly = 1) { repository.sendReady(playlistItemId, 0, false) }
		verify(exactly = 1) { repository.sendReady(playlistItemId, 10_000, false) }
	}
})
