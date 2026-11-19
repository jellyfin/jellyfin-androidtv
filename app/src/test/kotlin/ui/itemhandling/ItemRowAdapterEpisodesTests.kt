package org.jellyfin.androidtv.ui.itemhandling

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeout
import org.jellyfin.androidtv.ui.browsing.BrowsingUtils
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.operations.TvShowsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class ItemRowAdapterEpisodesTests : FunSpec({
	val query = BrowsingUtils.createNextEpisodesRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
	lateinit var adapter: ItemRowAdapter
	lateinit var api: ApiClient
	lateinit var showsApi: TvShowsApi
	lateinit var scopeJob: Job
	lateinit var finished: CompletableDeferred<Exception?>

	beforeTest {
		mockkObject(ProcessLifecycleOwner.Companion)
		mockkStatic("androidx.lifecycle.LifecycleOwnerKt")
		val owner = mockk<LifecycleOwner>()
		val scope = mockk<LifecycleCoroutineScope>()
		scopeJob = Job()
		every { ProcessLifecycleOwner.get() } returns owner
		every { owner.lifecycleScope } returns scope
		every { scope.coroutineContext } returns (scopeJob + Dispatchers.Unconfined)

		api = mockk()
		showsApi = mockk()
		every { api.tvShowsApi } returns showsApi
		adapter = mockk(relaxed = true)
		finished = CompletableDeferred()
		every { adapter.notifyRetrieveFinished() } answers {
			finished.complete(null)
			Unit
		}
		every { adapter.notifyRetrieveFinished(any()) } answers {
			finished.complete(firstArg())
			Unit
		}
	}

	afterTest {
		scopeJob.cancel()
		unmockkStatic("androidx.lifecycle.LifecycleOwnerKt")
		unmockkObject(ProcessLifecycleOwner.Companion)
	}

	for ((description, numbers) in listOf(
		"absolute numbering" to listOf(249),
		"gaps in numbering" to listOf(3, 5, 7),
	)) {
		test("episode retrieval preserves the returned cards and row for $description") {
			val episodes = numbers.map { number ->
				BaseItemDto(id = UUID.randomUUID(), type = BaseItemKind.EPISODE, indexNumber = number)
			}
			coEvery { showsApi.getEpisodes(query) } returns Response(
				BaseItemDtoQueryResult(items = episodes, totalRecordCount = episodes.size + 1, startIndex = 1),
				200,
				emptyMap(),
			)
			val rows = slot<List<Any>>()
			every { adapter.replaceAll(capture(rows), any(), any()) } returns Unit

			adapter.retrieveEpisodes(api, query)

			withTimeout(5_000) { finished.await() } shouldBe null
			rows.captured.map { (it as BaseItemDtoBaseRowItem).baseItem } shouldBe episodes
			coVerify(exactly = 1) { showsApi.getEpisodes(query) }
			verify(exactly = 1) { adapter.itemsLoaded = episodes.size }
			verify(exactly = 0) { adapter.removeRow() }
		}
	}

	test("episode retrieval removes the row when no following episodes remain") {
		coEvery { showsApi.getEpisodes(query) } returns Response(
			BaseItemDtoQueryResult(items = emptyList(), totalRecordCount = 1, startIndex = 1),
			200,
			emptyMap(),
		)

		adapter.retrieveEpisodes(api, query)

		withTimeout(5_000) { finished.await() } shouldBe null
		verify(exactly = 1) { adapter.removeRow() }
		verify(exactly = 1) { adapter.itemsLoaded = 0 }
	}

	test("episode retrieval reports failures without treating them as an empty season") {
		val error = IllegalStateException("Episode request failed")
		coEvery { showsApi.getEpisodes(query) } throws error

		adapter.retrieveEpisodes(api, query)

		withTimeout(5_000) { finished.await() } shouldBe error
		verify(exactly = 0) { adapter.removeRow() }
		verify(exactly = 0) { adapter.replaceAll(any(), any(), any()) }
	}
})
