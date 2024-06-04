package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest
import org.jellyfin.sdk.model.api.request.GetSeasonsRequest
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest
import org.jellyfin.sdk.model.api.request.GetUpcomingEpisodesRequest
import timber.log.Timber
import java.util.UUID

object BrowsingUtils {
	@JvmStatic
	fun getRandomItem(
		api: ApiClient,
		lifecycle: LifecycleOwner,
		library: BaseItemDto,
		type: BaseItemKind,
		callback: (item: BaseItemDto?) -> Unit
	) {
		lifecycle.lifecycleScope.launch {
			try {
				val result by api.itemsApi.getItems(
					parentId = library.id,
					includeItemTypes = setOf(type),
					recursive = true,
					sortBy = setOf(ItemSortBy.RANDOM),
					limit = 1,
				)

				callback(result.items?.firstOrNull())
			} catch (error: ApiClientException) {
				Timber.w(error, "Failed to retrieve random item")
				callback(null)
			}
		}
	}

	@JvmStatic
	fun createGetNextUpRequest(parentId: UUID) = GetNextUpRequest(
		limit = 50,
		parentId = parentId,
		imageTypeLimit = 1,
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.OVERVIEW,
			ItemFields.CHILD_COUNT,
			ItemFields.MEDIA_SOURCES,
			ItemFields.MEDIA_STREAMS,
		)
	)

	@JvmStatic
	fun createSeriesGetNextUpRequest(parentId: UUID) = GetNextUpRequest(
		seriesId = parentId,
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHILD_COUNT,
		)
	)

	@JvmStatic
	@JvmOverloads
	fun createLatestMediaRequest(
		parentId: UUID,
		itemType: BaseItemKind? = null,
		groupItems: Boolean? = null
	) = GetLatestMediaRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.OVERVIEW,
			ItemFields.CHILD_COUNT,
			ItemFields.MEDIA_SOURCES,
			ItemFields.MEDIA_STREAMS,
		),
		parentId = parentId,
		limit = 50,
		imageTypeLimit = 1,
		includeItemTypes = itemType?.let(::setOf),
		groupItems = groupItems,
	)

	@JvmStatic
	fun createSeasonsRequest(seriesId: UUID) = GetSeasonsRequest(
		seriesId = seriesId,
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
		),
	)

	@JvmStatic
	fun createUpcomingEpisodesRequest(parentId: UUID) = GetUpcomingEpisodesRequest(
		parentId = parentId,
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHILD_COUNT,
		),
	)

	@JvmStatic
	fun createSimilarItemsRequest(itemId: UUID) = GetSimilarItemsRequest(
		itemId = itemId,
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
		),
		limit = 20,
	)

	@JvmStatic
	fun createLiveTVOnNowRequest() = GetRecommendedProgramsRequest(
		isAiring = true,
		fields = setOf(
			ItemFields.OVERVIEW,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHANNEL_INFO,
			ItemFields.CHILD_COUNT,
		),
		imageTypeLimit = 1,
		enableTotalRecordCount = false,
		limit = 150,
	)

	@JvmStatic
	fun createLiveTVUpcomingRequest() = GetRecommendedProgramsRequest(
		isAiring = false,
		hasAired = false,
		fields = setOf(
			ItemFields.OVERVIEW,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHANNEL_INFO,
			ItemFields.CHILD_COUNT,
		),
		imageTypeLimit = 1,
		enableTotalRecordCount = false,
		limit = 150,
	)

	@JvmStatic
	@JvmOverloads
	fun createLiveTVRecordingsRequest(limit: Int? = null) = GetRecordingsRequest(
		fields = setOf(
			ItemFields.OVERVIEW,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHILD_COUNT,
		),
		enableImages = true,
		limit = limit,
	)

	@JvmStatic
	fun createLiveTVMovieRecordingsRequest() = GetRecordingsRequest(
		fields = setOf(
			ItemFields.OVERVIEW,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHILD_COUNT,
		),
		enableImages = true,
		limit = 60,
		isMovie = true,
	)

	@JvmStatic
	fun createLiveTVSeriesRecordingsRequest() = GetRecordingsRequest(
		fields = setOf(
			ItemFields.OVERVIEW,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHILD_COUNT,
		),
		enableImages = true,
		limit = 60,
		isSeries = true,
	)

	@JvmStatic
	fun createLiveTVSportsRecordingsRequest() = GetRecordingsRequest(
		fields = setOf(
			ItemFields.OVERVIEW,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHILD_COUNT,
		),
		enableImages = true,
		limit = 60,
		isSports = true,
	)

	@JvmStatic
	fun createLiveTVKidsRecordingsRequest() = GetRecordingsRequest(
		fields = setOf(
			ItemFields.OVERVIEW,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CHILD_COUNT,
		),
		enableImages = true,
		limit = 60,
		isKids = true,
	)
}
