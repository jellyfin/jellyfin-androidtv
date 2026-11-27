package org.jellyfin.androidtv.util.sdk

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.api.operations.TvShowsApi
import org.jellyfin.sdk.api.operations.VideosApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import java.util.UUID

private fun createItem(
	type: BaseItemKind,
	id: UUID = UUID.randomUUID(),
	seriesId: UUID? = null,
	partCount: Int? = null,
): BaseItemDto = mockk {
	every { this@mockk.id } returns id
	every { this@mockk.type } returns type
	every { this@mockk.seriesId } returns seriesId
	every { this@mockk.partCount } returns partCount
}

private fun createQueryResponse(items: List<BaseItemDto>): Response<BaseItemDtoQueryResult> {
	val result = mockk<BaseItemDtoQueryResult> {
		every { this@mockk.items } returns items
	}
	return Response(result, 200, emptyMap())
}

class SdkPlaybackHelperMultiPartTests : FunSpec({
	beforeSpec {
		mockkStatic("org.jellyfin.sdk.api.client.extensions.ApiClientExtensionsKt")
	}

	afterSpec {
		unmockkStatic("org.jellyfin.sdk.api.client.extensions.ApiClientExtensionsKt")
	}

	fun createHelper(
		api: ApiClient,
		mediaQueuingEnabled: Boolean = true,
	): SdkPlaybackHelper {
		val userPreferences = mockk<UserPreferences> {
			every { this@mockk[UserPreferences.mediaQueuingEnabled] } returns mediaQueuingEnabled
		}
		return SdkPlaybackHelper(
			api = api,
			userPreferences = userPreferences,
			playbackLauncher = mockk<PlaybackLauncher>(relaxed = true),
			playbackControllerContainer = mockk<PlaybackControllerContainer>(relaxed = true),
		)
	}

	test("getItems queues additional parts for multi-part episodes when media queuing is enabled") {
		val seriesId = UUID.randomUUID()
		val ep1Part1 = createItem(BaseItemKind.EPISODE, seriesId = seriesId, partCount = 2)
		val ep1Part2 = createItem(BaseItemKind.EPISODE, seriesId = seriesId, partCount = null)
		val ep2 = createItem(BaseItemKind.EPISODE, seriesId = seriesId, partCount = 1)

		val tvShowsApi = mockk<TvShowsApi>()
		val videosApi = mockk<VideosApi>()
		val api = mockk<ApiClient> {
			every { this@mockk.tvShowsApi } returns tvShowsApi
			every { this@mockk.videosApi } returns videosApi
		}

		coEvery {
			tvShowsApi.getEpisodes(
				seriesId = seriesId,
				startItemId = ep1Part1.id,
				isMissing = false,
				limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
				fields = ItemRepository.itemFields,
			)
		} returns createQueryResponse(listOf(ep1Part1, ep2))
		coEvery { videosApi.getAdditionalPart(ep1Part1.id) } returns createQueryResponse(listOf(ep1Part2))

		val helper = createHelper(api, mediaQueuingEnabled = true)
		val items = helper.getItems(ep1Part1, allowIntros = false, shuffle = false)

		val ep1Id = ep1Part1.id
		val ep2Id = ep2.id
		items shouldBe listOf(ep1Part1, ep1Part2, ep2)
		coVerify(exactly = 1) { videosApi.getAdditionalPart(ep1Id) }
		coVerify(exactly = 0) { videosApi.getAdditionalPart(ep2Id) }
	}

	test("getItems queues additional parts for a multi-part episode when media queuing is disabled") {
		val seriesId = UUID.randomUUID()
		val ep1Part1 = createItem(BaseItemKind.EPISODE, seriesId = seriesId, partCount = 2)
		val ep1Part2 = createItem(BaseItemKind.EPISODE, seriesId = seriesId, partCount = null)

		val videosApi = mockk<VideosApi>()
		val api = mockk<ApiClient> {
			every { this@mockk.videosApi } returns videosApi
		}
		coEvery { videosApi.getAdditionalPart(ep1Part1.id) } returns createQueryResponse(listOf(ep1Part2))

		val helper = createHelper(api, mediaQueuingEnabled = false)
		val items = helper.getItems(ep1Part1, allowIntros = false, shuffle = false)

		items shouldBe listOf(ep1Part1, ep1Part2)
	}

	test("getItems queues additional parts when playing a season") {
		val seriesId = UUID.randomUUID()
		val season = createItem(BaseItemKind.SEASON, seriesId = seriesId)
		val ep1 = createItem(BaseItemKind.EPISODE, seriesId = seriesId, partCount = null)
		val ep2Part1 = createItem(BaseItemKind.EPISODE, seriesId = seriesId, partCount = 2)
		val ep2Part2 = createItem(BaseItemKind.EPISODE, seriesId = seriesId, partCount = null)

		val tvShowsApi = mockk<TvShowsApi>()
		val videosApi = mockk<VideosApi>()
		val api = mockk<ApiClient> {
			every { this@mockk.tvShowsApi } returns tvShowsApi
			every { this@mockk.videosApi } returns videosApi
		}

		coEvery {
			tvShowsApi.getEpisodes(
				seriesId = seriesId,
				seasonId = season.id,
				isMissing = false,
				sortBy = ItemSortBy.SORT_NAME,
				limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
				fields = ItemRepository.itemFields,
			)
		} returns createQueryResponse(listOf(ep1, ep2Part1))
		coEvery { videosApi.getAdditionalPart(ep2Part1.id) } returns createQueryResponse(listOf(ep2Part2))

		val helper = createHelper(api)
		val items = helper.getItems(season, allowIntros = false, shuffle = false)

		items shouldBe listOf(ep1, ep2Part1, ep2Part2)
	}

	test("getItems queues additional parts when playing a series") {
		val series = createItem(BaseItemKind.SERIES)
		val ep1Part1 = createItem(BaseItemKind.EPISODE, seriesId = series.id, partCount = 2)
		val ep1Part2 = createItem(BaseItemKind.EPISODE, seriesId = series.id, partCount = null)
		val ep2 = createItem(BaseItemKind.EPISODE, seriesId = series.id, partCount = null)

		val tvShowsApi = mockk<TvShowsApi>()
		val videosApi = mockk<VideosApi>()
		val api = mockk<ApiClient> {
			every { this@mockk.tvShowsApi } returns tvShowsApi
			every { this@mockk.videosApi } returns videosApi
		}

		coEvery {
			tvShowsApi.getEpisodes(
				seriesId = series.id,
				isMissing = false,
				sortBy = ItemSortBy.SORT_NAME,
				limit = SdkPlaybackHelper.ITEM_QUERY_LIMIT,
				fields = ItemRepository.itemFields,
			)
		} returns createQueryResponse(listOf(ep1Part1, ep2))
		coEvery { videosApi.getAdditionalPart(ep1Part1.id) } returns createQueryResponse(listOf(ep1Part2))

		val helper = createHelper(api)
		val items = helper.getItems(series, allowIntros = false, shuffle = false)

		items shouldBe listOf(ep1Part1, ep1Part2, ep2)
	}
})
