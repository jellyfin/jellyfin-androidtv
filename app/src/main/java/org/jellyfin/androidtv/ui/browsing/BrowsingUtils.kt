package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
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
		lifecycle.lifecycleScope.launch(Dispatchers.IO) {
			try {
				val result by api.itemsApi.getItems(
					parentId = library.id,
					includeItemTypes = setOf(type),
					recursive = true,
					sortBy = setOf(ItemSortBy.RANDOM),
					limit = 1,
				)

				withContext(Dispatchers.Main) {
					callback(result.items.firstOrNull())
				}
			} catch (error: ApiClientException) {
				Timber.w(error, "Failed to retrieve random item")

				withContext(Dispatchers.Main) {
					callback(null)
				}
			}
		}
	}

	@JvmStatic
	fun createGetNextUpRequest(parentId: UUID) = GetNextUpRequest(
		limit = 50,
		parentId = parentId,
		imageTypeLimit = 1,
		fields = ItemRepository.itemFields
	)

	@JvmStatic
	fun createSeriesGetNextUpRequest(parentId: UUID) = GetNextUpRequest(
		seriesId = parentId,
		fields = ItemRepository.itemFields
	)

	@JvmStatic
	@JvmOverloads
	fun createLatestMediaRequest(
		parentId: UUID,
		itemType: BaseItemKind? = null,
		groupItems: Boolean? = null
	) = GetLatestMediaRequest(
		fields = ItemRepository.itemFields,
		parentId = parentId,
		limit = 50,
		imageTypeLimit = 1,
		includeItemTypes = itemType?.let(::setOf),
		groupItems = groupItems,
	)

	@JvmStatic
	fun createSeasonsRequest(seriesId: UUID) = GetSeasonsRequest(
		seriesId = seriesId,
		fields = ItemRepository.itemFields,
	)

	@JvmStatic
	fun createUpcomingEpisodesRequest(parentId: UUID) = GetUpcomingEpisodesRequest(
		parentId = parentId,
		fields = ItemRepository.itemFields,
	)

	@JvmStatic
	fun createSimilarItemsRequest(itemId: UUID) = GetSimilarItemsRequest(
		itemId = itemId,
		fields = ItemRepository.itemFields,
		limit = 20,
	)

	@JvmStatic
	fun createLiveTVOnNowRequest() = GetRecommendedProgramsRequest(
		isAiring = true,
		fields = ItemRepository.itemFields,
		imageTypeLimit = 1,
		enableTotalRecordCount = false,
		limit = 150,
	)

	@JvmStatic
	fun createLiveTVUpcomingRequest() = GetRecommendedProgramsRequest(
		isAiring = false,
		hasAired = false,
		fields = ItemRepository.itemFields,
		imageTypeLimit = 1,
		enableTotalRecordCount = false,
		limit = 150,
	)

	@JvmStatic
	@JvmOverloads
	fun createLiveTVRecordingsRequest(limit: Int? = null) = GetRecordingsRequest(
		fields = ItemRepository.itemFields,
		enableImages = true,
		limit = limit,
	)

	@JvmStatic
	fun createLiveTVMovieRecordingsRequest() = GetRecordingsRequest(
		fields = ItemRepository.itemFields,
		enableImages = true,
		limit = 60,
		isMovie = true,
	)

	@JvmStatic
	fun createLiveTVSeriesRecordingsRequest() = GetRecordingsRequest(
		fields = ItemRepository.itemFields,
		enableImages = true,
		limit = 60,
		isSeries = true,
	)

	@JvmStatic
	fun createLiveTVSportsRecordingsRequest() = GetRecordingsRequest(
		fields = ItemRepository.itemFields,
		enableImages = true,
		limit = 60,
		isSports = true,
	)

	@JvmStatic
	fun createLiveTVKidsRecordingsRequest() = GetRecordingsRequest(
		fields = ItemRepository.itemFields,
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
		fields = ItemRepository.itemFields,
		parentId = parentId,
	)

	@JvmStatic
	fun createArtistsRequest(parentId: UUID) = GetArtistsRequest(
		fields = ItemRepository.itemFields,
		parentId = parentId,
	)

	@JvmStatic
	fun createPersonItemsRequest(personId: UUID, itemType: BaseItemKind) = GetItemsRequest(
		fields = ItemRepository.itemFields,
		personIds = setOf(personId),
		recursive = true,
		includeItemTypes = setOf(itemType),
		sortBy = setOf(ItemSortBy.SORT_NAME),
	)

	@JvmStatic
	fun createArtistItemsRequest(artistId: UUID, itemType: BaseItemKind) = GetItemsRequest(
		fields = ItemRepository.itemFields,
		artistIds = setOf(artistId),
		recursive = true,
		includeItemTypes = setOf(itemType),
		sortBy = setOf(ItemSortBy.SORT_NAME),
	)

	@JvmStatic
	fun createNextEpisodesRequest(seasonId: UUID, indexNumber: Int) = GetItemsRequest(
		fields = ItemRepository.itemFields,
		parentId = seasonId,
		includeItemTypes = setOf(BaseItemKind.EPISODE),
		startIndex = indexNumber,
		limit = 20,
	)

	@JvmStatic
	fun createResumeItemsRequest(parentId: UUID, itemType: BaseItemKind) = GetItemsRequest(
		fields = ItemRepository.itemFields,
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
		fields = ItemRepository.itemFields,
		includeItemTypes = setOf(itemType),
		recursive = true,
		parentId = parentId,
		imageTypeLimit = 1,
		filters = setOf(ItemFilter.IS_FAVORITE),
		sortBy = setOf(ItemSortBy.SORT_NAME),
	)

	@JvmStatic
	fun createCollectionsRequest(parentId: UUID) = GetItemsRequest(
		fields = ItemRepository.itemFields,
		includeItemTypes = setOf(BaseItemKind.BOX_SET),
		recursive = true,
		imageTypeLimit = 1,
		parentId = parentId,
		sortBy = setOf(ItemSortBy.SORT_NAME),
	)

	@JvmStatic
	fun createPremieresRequest(parentId: UUID) = GetItemsRequest(
		fields = ItemRepository.itemFields,
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
		fields = ItemRepository.itemFields,
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
		fields = ItemRepository.itemFields,
		includeItemTypes = setOf(BaseItemKind.PLAYLIST),
		imageTypeLimit = 1,
		recursive = true,
		sortBy = setOf(ItemSortBy.DATE_CREATED),
		sortOrder = setOf(SortOrder.DESCENDING),
	)

	@JvmStatic
	fun createBrowseGridItemsRequest(parent: BaseItemDto): GetItemsRequest {
		val baseRequest = GetItemsRequest(
			fields = ItemRepository.itemFields,
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
