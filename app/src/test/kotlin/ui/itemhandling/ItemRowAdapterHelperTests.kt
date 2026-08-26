package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import androidx.leanback.widget.Presenter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import org.jellyfin.androidtv.util.apiclient.EmptyResponse
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.operations.TvShowsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ItemRowAdapterHelperTests : FunSpec({
	// retrieveNextUpItems launches into ProcessLifecycleOwner's real lifecycleScope. Accessing that
	// scope for the first time anywhere in the JVM makes androidx's LifecycleCoroutineScopeImpl
	// register itself as a Lifecycle observer via a coroutine dispatched onto Dispatchers.Main -
	// which calls into android.os.Looper, unmocked in this Robolectric-less JVM unit test. That
	// throws asynchronously with no attached exception handler, so kotlinx-coroutines-test reports
	// it as a leaked "uncaught exception" against whatever unrelated test happens to run next
	// (this bit UpdateLaunchGateTest, since it isn't inherently connected to this test at all).
	//
	// To keep this test hermetic we mock the `LifecycleOwner.lifecycleScope` extension so
	// retrieveNextUpItems launches into a scope this test fully owns, never touching the real
	// process-global Lifecycle/Looper machinery.
	val executor = Executors.newSingleThreadExecutor()
	val dispatcher = executor.asCoroutineDispatcher()
	val testLifecycleScope = mockk<LifecycleCoroutineScope>(relaxed = true)
	every { testLifecycleScope.coroutineContext } returns dispatcher + Job()

	beforeEach {
		mockkStatic("org.jellyfin.sdk.api.client.extensions.ApiClientExtensionsKt")
		mockkStatic("androidx.lifecycle.LifecycleOwnerKt")
		every { any<LifecycleOwner>().lifecycleScope } returns testLifecycleScope
	}

	afterEach {
		unmockkStatic("androidx.lifecycle.LifecycleOwnerKt")
		unmockkStatic("org.jellyfin.sdk.api.client.extensions.ApiClientExtensionsKt")
	}

	afterSpec {
		executor.shutdown()
	}

	// Regression test for a bug where a season with a numbering gap before its first available
	// episode (e.g. only episodes 10-30 exist on disk, 1-9 missing) caused the "Next Up" row to
	// silently skip episodes. The old code used `itemsApi.getItems(startIndex = firstNextUp.indexNumber)`,
	// where startIndex is a pagination offset (skip N items) rather than a filter on IndexNumber,
	// so it landed on the wrong item and dropped everything in between. The fix uses
	// `tvShowsApi.getEpisodes(startItemId = firstNextUp.id)`, which resolves by item id instead.
	test("retrieveNextUpItems returns all episodes when the season has a numbering gap before its first available episode") {
		val seriesId = UUID.randomUUID()
		val seasonId = UUID.randomUUID()

		// Only episodes 10..30 are present on disk (E1-E9 missing) - this matches the real-world
		// "Gomer Pyle, U.S.M.C." Season 5 scenario that exposed the bug.
		val episodes = (10..30).map { index ->
			BaseItemDto(
				id = UUID.randomUUID(),
				type = BaseItemKind.EPISODE,
				mediaType = MediaType.VIDEO,
				seriesId = seriesId,
				seasonId = seasonId,
				indexNumber = index,
			)
		}
		val firstNextUp = episodes.first()

		val api = mockk<ApiClient>()
		val tvShowsApi = mockk<TvShowsApi>()
		every { api.tvShowsApi } returns tvShowsApi

		coEvery {
			tvShowsApi.getNextUp(any<GetNextUpRequest>())
		} returns Response(
			content = BaseItemDtoQueryResult(items = listOf(firstNextUp), totalRecordCount = 1, startIndex = 0),
			status = 200,
			headers = emptyMap(),
		)

		val seriesIdSlot = slot<UUID>()
		val startItemIdSlot = slot<UUID>()
		val isMissingSlot = slot<Boolean>()
		coEvery {
			tvShowsApi.getEpisodes(
				seriesId = capture(seriesIdSlot),
				userId = any(),
				fields = any(),
				season = any(),
				seasonId = any(),
				isMissing = capture(isMissingSlot),
				adjacentTo = any(),
				startItemId = capture(startItemIdSlot),
				startIndex = any(),
				limit = any(),
				enableImages = any(),
				imageTypeLimit = any(),
				enableImageTypes = any(),
				enableUserData = any(),
				sortBy = any(),
			)
		} returns Response(
			content = BaseItemDtoQueryResult(items = episodes, totalRecordCount = episodes.size, startIndex = 0),
			status = 200,
			headers = emptyMap(),
		)

		val adapter = ItemRowAdapter(
			mockk<Context>(relaxed = true),
			GetNextUpRequest(seriesId = seriesId),
			false,
			mockk<Presenter>(relaxed = true),
			null,
		)

		val latch = CountDownLatch(1)
		adapter.setRetrieveFinishedListener(object : EmptyResponse(mockk<Lifecycle>(relaxed = true)) {
			override fun onResponse() = latch.countDown()
			override fun onError(exception: Exception) = latch.countDown()
		})

		adapter.retrieveNextUpItems(api, GetNextUpRequest(seriesId = seriesId))

		latch.await(10, TimeUnit.SECONDS) shouldBe true

		// Confirm the fixed lookup pattern was used: episode identity, not position, drives the query.
		seriesIdSlot.captured shouldBe seriesId
		startItemIdSlot.captured shouldBe firstNextUp.id
		isMissingSlot.captured shouldBe false

		// This is the actual regression assertion: all 21 episodes (E10..E30) must be present, with
		// nothing skipped. Against the old buggy code this would have produced only 12 items
		// (E10 + E20..E30), missing E11-E19.
		adapter.size() shouldBe episodes.size

		val resultIndexNumbers = (0 until adapter.size()).map {
			(adapter.get(it) as BaseItemDtoBaseRowItem).baseItem?.indexNumber
		}
		resultIndexNumbers shouldBe (10..30).toList()
	}
})
