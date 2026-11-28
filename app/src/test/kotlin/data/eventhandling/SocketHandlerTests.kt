package org.jellyfin.androidtv.data.eventhandling

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope
import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.util.PlaybackHelper
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlayCommand
import org.jellyfin.sdk.model.api.PlayMessage
import org.jellyfin.sdk.model.api.PlayRequest
import java.util.UUID
import kotlin.coroutines.CoroutineContext

private object ImmediateMainDispatcher : MainCoroutineDispatcher() {
	override val immediate: MainCoroutineDispatcher get() = this
	override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
}

class SocketHandlerTests : FunSpec({
	beforeSpec {
		mockkStatic(Dispatchers::class)
		every { Dispatchers.Main } returns ImmediateMainDispatcher
		mockkStatic("androidx.lifecycle.LifecycleKt")
		mockkStatic("org.jellyfin.sdk.api.client.extensions.ApiClientExtensionsKt")
	}

	afterSpec {
		unmockkStatic(Dispatchers::class)
		unmockkStatic("androidx.lifecycle.LifecycleKt")
		unmockkStatic("org.jellyfin.sdk.api.client.extensions.ApiClientExtensionsKt")
	}

	fun createHandler(
		context: Context = mockk(relaxed = true),
		api: ApiClient = mockk(relaxed = true),
		mediaManager: MediaManager = mockk(relaxed = true),
		playbackHelper: PlaybackHelper = mockk(relaxed = true),
	): SocketHandler {
		val lifecycle = mockk<Lifecycle>(relaxed = true)
		val scope = mockk<LifecycleCoroutineScope>(relaxed = true) {
			every { coroutineContext } returns Job().apply { cancel() }
		}
		every { lifecycle.coroutineScope } returns scope

		return SocketHandler(
			context = context,
			api = api,
			dataRefreshService = mockk<DataRefreshService>(relaxed = true),
			mediaManager = mediaManager,
			playbackControllerContainer = mockk<PlaybackControllerContainer>(relaxed = true),
			navigationRepository = mockk<NavigationRepository>(relaxed = true),
			audioManager = mockk<AudioManager>(relaxed = true),
			playbackHelper = playbackHelper,
			lifecycle = lifecycle,
		)
	}

	test("onPlayMessage with PLAY_LAST appends audio items to queue instead of playing immediately") {
		val itemId = UUID.randomUUID()
		val audioItem = mockk<BaseItemDto> {
			every { mediaType } returns MediaType.AUDIO
		}
		val itemsApi = mockk<ItemsApi>()
		val api = mockk<ApiClient>()
		every { api.itemsApi } returns itemsApi
		val queryResult = mockk<BaseItemDtoQueryResult> {
			every { items } returns listOf(audioItem)
		}
		val response = mockk<Response<BaseItemDtoQueryResult>> {
			every { content } returns queryResult
		}
		coEvery {
			itemsApi.getItems(
				ids = listOf(itemId),
				fields = ItemRepository.itemFields,
			)
		} returns response

		val mediaManager = mockk<MediaManager>(relaxed = true)
		val playbackHelper = mockk<PlaybackHelper>(relaxed = true)
		val handler = createHandler(api = api, mediaManager = mediaManager, playbackHelper = playbackHelper)

		val message = PlayMessage(
			data = PlayRequest(
				itemIds = listOf(itemId),
				playCommand = PlayCommand.PLAY_LAST,
				controllingUserId = UUID.randomUUID(),
			),
			messageId = UUID.randomUUID(),
		)

		handler.onPlayMessage(message)

		verify(exactly = 1) { mediaManager.addToAudioQueue(listOf(audioItem)) }
		verify(exactly = 0) { playbackHelper.retrieveAndPlay(any<List<UUID>>(), any(), any(), any(), any()) }
	}

	test("onPlayMessage with PLAY_LAST falls back to retrieveAndPlay for non-audio items") {
		val itemId = UUID.randomUUID()
		val videoItem = mockk<BaseItemDto> {
			every { mediaType } returns MediaType.VIDEO
		}
		val itemsApi = mockk<ItemsApi>()
		val api = mockk<ApiClient>()
		every { api.itemsApi } returns itemsApi
		val queryResult = mockk<BaseItemDtoQueryResult> {
			every { items } returns listOf(videoItem)
		}
		val response = mockk<Response<BaseItemDtoQueryResult>> {
			every { content } returns queryResult
		}
		coEvery {
			itemsApi.getItems(
				ids = listOf(itemId),
				fields = ItemRepository.itemFields,
			)
		} returns response

		val context = mockk<Context>(relaxed = true)
		val mediaManager = mockk<MediaManager>(relaxed = true)
		val playbackHelper = mockk<PlaybackHelper>(relaxed = true)
		val handler = createHandler(
			context = context,
			api = api,
			mediaManager = mediaManager,
			playbackHelper = playbackHelper,
		)

		val message = PlayMessage(
			data = PlayRequest(
				itemIds = listOf(itemId),
				playCommand = PlayCommand.PLAY_LAST,
				controllingUserId = UUID.randomUUID(),
				startPositionTicks = 1234L,
				startIndex = 0,
			),
			messageId = UUID.randomUUID(),
		)

		handler.onPlayMessage(message)

		verify(exactly = 0) { mediaManager.addToAudioQueue(any()) }
		verify(exactly = 1) { playbackHelper.retrieveAndPlay(listOf(itemId), false, 1234L, 0, context) }
	}

	test("onPlayMessage with PLAY_LAST does nothing when server returns no items") {
		val itemId = UUID.randomUUID()
		val itemsApi = mockk<ItemsApi>()
		val api = mockk<ApiClient>()
		every { api.itemsApi } returns itemsApi
		val queryResult = mockk<BaseItemDtoQueryResult> {
			every { items } returns emptyList()
		}
		val response = mockk<Response<BaseItemDtoQueryResult>> {
			every { content } returns queryResult
		}
		coEvery {
			itemsApi.getItems(
				ids = listOf(itemId),
				fields = ItemRepository.itemFields,
			)
		} returns response

		val mediaManager = mockk<MediaManager>(relaxed = true)
		val playbackHelper = mockk<PlaybackHelper>(relaxed = true)
		val handler = createHandler(api = api, mediaManager = mediaManager, playbackHelper = playbackHelper)

		val message = PlayMessage(
			data = PlayRequest(
				itemIds = listOf(itemId),
				playCommand = PlayCommand.PLAY_LAST,
				controllingUserId = UUID.randomUUID(),
			),
			messageId = UUID.randomUUID(),
		)

		handler.onPlayMessage(message)

		verify(exactly = 0) { mediaManager.addToAudioQueue(any()) }
		verify(exactly = 0) { playbackHelper.retrieveAndPlay(any<List<UUID>>(), any(), any(), any(), any()) }
	}

	test("onPlayMessage with PLAY_NOW starts playback immediately") {
		val itemId = UUID.randomUUID()
		val context = mockk<Context>(relaxed = true)
		val mediaManager = mockk<MediaManager>(relaxed = true)
		val playbackHelper = mockk<PlaybackHelper>(relaxed = true)
		val handler = createHandler(
			context = context,
			mediaManager = mediaManager,
			playbackHelper = playbackHelper,
		)

		val message = PlayMessage(
			data = PlayRequest(
				itemIds = listOf(itemId),
				playCommand = PlayCommand.PLAY_NOW,
				controllingUserId = UUID.randomUUID(),
				startPositionTicks = 500L,
				startIndex = 1,
			),
			messageId = UUID.randomUUID(),
		)

		handler.onPlayMessage(message)

		verify(exactly = 0) { mediaManager.addToAudioQueue(any()) }
		verify(exactly = 1) { playbackHelper.retrieveAndPlay(listOf(itemId), false, 500L, 1, context) }
	}
})
