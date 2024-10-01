package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetAlbumArtistsRequest
import org.jellyfin.sdk.model.api.request.GetArtistsRequest
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetLiveTvChannelsRequest
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

	@JvmStatic
	fun createLiveTVChannelsRequest(isFavorite: Boolean) = GetLiveTvChannelsRequest(
		isFavorite = isFavorite,
	)

	@JvmStatic
	fun createAlbumArtistsRequest(parentId: UUID) = GetAlbumArtistsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.ITEM_COUNTS,
			ItemFields.CHILD_COUNT,
		),
		parentId = parentId,
	)

	@JvmStatic
	fun createArtistsRequest(parentId: UUID) = GetArtistsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.ITEM_COUNTS,
			ItemFields.CHILD_COUNT,
		),
		parentId = parentId,
	)

	@JvmStatic
	fun createPersonItemsRequest(personId: UUID, itemType: BaseItemKind) = GetItemsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
		),
		personIds = setOf(personId),
		recursive = true,
		includeItemTypes = setOf(itemType),
		sortBy = setOf(ItemSortBy.SORT_NAME),
	)

	@JvmStatic
	fun createArtistItemsRequest(artistId: UUID, itemType: BaseItemKind) = GetItemsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
		),
		artistIds = setOf(artistId),
		recursive = true,
		includeItemTypes = setOf(itemType),
		sortBy = setOf(ItemSortBy.SORT_NAME),
	)

	@JvmStatic
	fun createNextEpisodesRequest(seasonId: UUID, indexNumber: Int) = GetItemsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.OVERVIEW,
			ItemFields.ITEM_COUNTS,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
		),
		parentId = seasonId,
		includeItemTypes = setOf(BaseItemKind.EPISODE),
		startIndex = indexNumber,
		limit = 20,
	)

	@JvmStatic
	fun createResumeItemsRequest(parentId: UUID, itemType: BaseItemKind) = GetItemsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.OVERVIEW,
			ItemFields.ITEM_COUNTS,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
			ItemFields.MEDIA_STREAMS,
			ItemFields.MEDIA_SOURCES,
		),
		includeItemTypes = setOf(itemType),
		recursive = true,
		parentId = parentId,
		imageTypeLimit = 1,
		limit = 50,
		collapseBoxSetItems = false,
		enableTotalRecordCount = false,
		filters = setOf(ItemFilter.IS_RESUMABLE),
		sortBy = setOf(ItemSortBy.DATE_PLAYED),
		sortOrder = setOf(SortOrder.DESCENDING),
	)

	@JvmStatic
	fun createFavoriteItemsRequest(parentId: UUID, itemType: BaseItemKind) = GetItemsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.OVERVIEW,
			ItemFields.ITEM_COUNTS,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
			ItemFields.MEDIA_STREAMS,
			ItemFields.MEDIA_SOURCES,
		),
		includeItemTypes = setOf(itemType),
		recursive = true,
		parentId = parentId,
		imageTypeLimit = 1,
		filters = setOf(ItemFilter.IS_FAVORITE),
		sortBy = setOf(ItemSortBy.SORT_NAME),
	)

	@JvmStatic
	fun createCollectionsRequest(parentId: UUID) = GetItemsRequest(
		fields = setOf(ItemFields.CHILD_COUNT),
		includeItemTypes = setOf(BaseItemKind.BOX_SET),
		recursive = true,
		imageTypeLimit = 1,
		parentId = parentId,
		sortBy = setOf(ItemSortBy.SORT_NAME),
	)

	@JvmStatic
	fun createPremieresRequest(parentId: UUID) = GetItemsRequest(
		fields = setOf(
			ItemFields.DATE_CREATED,
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.OVERVIEW,
			ItemFields.CHILD_COUNT,
		),
		includeItemTypes = setOf(BaseItemKind.EPISODE),
		parentId = parentId,
		indexNumber = 1,
		recursive = true,
		isMissing = false,
		imageTypeLimit = 1,
		filters = setOf(ItemFilter.IS_UNPLAYED),
		sortBy = setOf(ItemSortBy.DATE_CREATED),
		sortOrder = setOf(SortOrder.DESCENDING),
		enableTotalRecordCount = false,
		limit = 300,
	)

	@JvmStatic
	fun createLastPlayedRequest(parentId: UUID) = GetItemsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.OVERVIEW,
			ItemFields.ITEM_COUNTS,
			ItemFields.DISPLAY_PREFERENCES_ID,
			ItemFields.CHILD_COUNT,
		),
		includeItemTypes = setOf(BaseItemKind.AUDIO),
		recursive = true,
		parentId = parentId,
		imageTypeLimit = 1,
		filters = setOf(ItemFilter.IS_PLAYED),
		sortBy = setOf(ItemSortBy.DATE_PLAYED),
		sortOrder = setOf(SortOrder.DESCENDING),
		enableTotalRecordCount = false,
		limit = 50,
	)

	@JvmStatic
	fun createPlaylistsRequest() = GetItemsRequest(
		fields = setOf(
			ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
			ItemFields.CUMULATIVE_RUN_TIME_TICKS,
			ItemFields.CHILD_COUNT,
		),
		includeItemTypes = setOf(BaseItemKind.PLAYLIST),
		imageTypeLimit = 1,
		recursive = true,
		sortBy = setOf(ItemSortBy.DATE_CREATED),
		sortOrder = setOf(SortOrder.DESCENDING),
	)

	@JvmStatic
	fun createBrowseGridItemsRequest(parent: BaseItemDto): GetItemsRequest {
		val baseRequest = GetItemsRequest(
			fields = setOf(
				ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
				ItemFields.CHILD_COUNT,
				ItemFields.MEDIA_SOURCES,
				ItemFields.MEDIA_STREAMS,
				ItemFields.DISPLAY_PREFERENCES_ID,
			),
			parentId = parent.id,
		)

		if (parent.type == BaseItemKind.USER_VIEW || parent.type == BaseItemKind.COLLECTION_FOLDER) {
			return when (parent.collectionType) {
				CollectionType.MOVIES -> baseRequest.copy(
					includeItemTypes = setOf(BaseItemKind.MOVIE),
					recursive = true,
				)

				CollectionType.TVSHOWS -> baseRequest.copy(
					includeItemTypes = setOf(BaseItemKind.SERIES),
					recursive = true,
				)

				CollectionType.BOXSETS -> baseRequest.copy(
					includeItemTypes = setOf(BaseItemKind.BOX_SET),
					parentId = null,
					recursive = true,
				)

				CollectionType.MUSIC -> baseRequest.copy(
					includeItemTypes = setOf(BaseItemKind.MUSIC_ALBUM),
					recursive = true,
				)

				else -> baseRequest
			}
		}

		return baseRequest
	}
}
