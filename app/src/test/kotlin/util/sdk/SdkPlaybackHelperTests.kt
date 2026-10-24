package org.jellyfin.androidtv.util.sdk

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import java.util.UUID

class SdkPlaybackHelperTests : BehaviorSpec({
	beforeSpec {
		mockkStatic("androidx.lifecycle.LifecycleOwnerKt")
		mockkStatic(Toast::class)
	}
	afterSpec {
		unmockkStatic("androidx.lifecycle.LifecycleOwnerKt")
		unmockkStatic(Toast::class)
	}
	Given("a remote PlayNow while a video is playing") {
		Then("playback fields are requested and the complete queue, position and index reach the player") {
			val items = List(2) { episode(listOf(mockk())) }
			val f = RemotePlaybackFixture(items)
			f.play(items.map { it.id }, position = 123_000_000, index = 1)
			coVerify { f.itemsApi.getItems(ids = items.map { it.id }, fields = ItemRepository.itemFields) }
			verify { f.launcher.launch(f.context, items, 12300, true, 1, false) }
			verify(exactly = 0) { f.toast.show() }
		}
		Then("a live TV placeholder source does not need probed streams to launch") {
			val source = mockk<MediaSourceInfo> { every { mediaStreams } returns emptyList() }
			val channel = BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.TV_CHANNEL, mediaSources = listOf(source))
			val f = RemotePlaybackFixture(listOf(channel))
			f.play(listOf(channel.id))
			verify { f.launcher.launch(f.context, listOf(channel), 0, true, 0, false) }
		}
		for ((name, items) in listOf(
			"unprobed item" to listOf(episode(null)),
			"empty sources later in the queue" to listOf(episode(listOf(mockk())), episode(emptyList())),
			"empty response" to emptyList(),
		)) {
			Then("$name reports failure without replacing the current playback") {
				val f = RemotePlaybackFixture(items)
				f.play(listOf(UUID.randomUUID()))
				verify { Toast.makeText(f.context, R.string.msg_cannot_play, Toast.LENGTH_LONG); f.toast.show() }
				verify { f.launcher wasNot Called }
				verify { f.controller wasNot Called }
			}
		}
	}
})

private class RemotePlaybackFixture(items: List<BaseItemDto>) {
	val context = mockk<Context>(moreInterfaces = arrayOf(LifecycleOwner::class))
	val itemsApi = mockk<ItemsApi>()
	val launcher = mockk<PlaybackLauncher>(relaxed = true)
	val controller = mockk<PlaybackController>(relaxed = true)
	val toast = mockk<Toast>(relaxed = true)
	private val job = SupervisorJob()
	private val failures = mutableListOf<Throwable>()
	private val helper: SdkPlaybackHelper
	init {
		val scope = mockk<LifecycleCoroutineScope>()
		every { scope.coroutineContext } returns (
			job + Dispatchers.Unconfined + CoroutineExceptionHandler { _, error -> failures.add(error) }
		)
		every { (context as LifecycleOwner).lifecycleScope } returns scope
		every { Toast.makeText(context, R.string.msg_cannot_play, Toast.LENGTH_LONG) } returns toast
		val api = mockk<ApiClient>()
		every { api.itemsApi } returns itemsApi
		coEvery { itemsApi.getItems(ids = any(), fields = any()) } returns Response(
			BaseItemDtoQueryResult(items = items, totalRecordCount = items.size, startIndex = 0), 200, emptyMap()
		)
		val preferences = mockk<UserPreferences>(relaxed = true)
		every { preferences[UserPreferences.resumeSubtractDuration] } returns "0"
		every { controller.hasFragment() } returns true
		helper = SdkPlaybackHelper(
			api, preferences, launcher, PlaybackControllerContainer().apply { playbackController = controller }
		)
	}
	suspend fun play(ids: List<UUID>, position: Long? = null, index: Int? = null) {
		try {
			helper.retrieveAndPlay(ids, false, position, index, context)
			job.children.toList().joinAll()
			failures shouldBe emptyList()
		} finally { job.cancel() }
	}
}

private fun episode(sources: List<MediaSourceInfo>?) = BaseItemDto(
	id = UUID.randomUUID(), type = BaseItemKind.EPISODE, mediaSources = sources
)
