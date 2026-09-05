package org.jellyfin.androidtv.ui.favorites

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber

interface FavoritesRepository {
	suspend fun getFavorites(
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>>
}

class FavoritesRepositoryImpl(
	private val apiClient: ApiClient
) : FavoritesRepository {
	companion object {
		private const val QUERY_LIMIT = 50
	}

	override suspend fun getFavorites(
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>> = try {
		val request = GetItemsRequest(
			limit = QUERY_LIMIT,
			imageTypeLimit = 1,
			includeItemTypes = itemTypes,
			filters = setOf(ItemFilter.IS_FAVORITE_OR_LIKES),
			sortBy = setOf(ItemSortBy.SORT_NAME),
			fields = ItemRepository.itemFields,
			recursive = true,
			enableTotalRecordCount = false,
		)

		val result = withContext(Dispatchers.IO) {
			apiClient.itemsApi.getItems(request).content
		}

		Result.success(result.items)
	} catch (e: ApiClientException) {
		Timber.e(e, "Failed to load favorite items")
		Result.failure(e)
	}
}
