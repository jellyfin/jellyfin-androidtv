package org.jellyfin.androidtv.ui.search

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import timber.log.Timber

interface SearchRepository {
	suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>
	): Result<List<BaseItemDto>?>
}

class SearchRepositoryImpl(
	private val apiClient: ApiClient
) : SearchRepository {
	companion object {
		private const val QUERY_LIMIT = 25
	}

	override suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>
	): Result<List<BaseItemDto>?> {
		return try {
			val result = apiClient.itemsApi.getItemsByUserId(
				searchTerm = searchTerm,
				limit = QUERY_LIMIT,
				imageTypeLimit = 1,
				includeItemTypes = itemTypes,
				fields = listOf(
					ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
					ItemFields.CAN_DELETE,
					ItemFields.BASIC_SYNC_INFO,
					ItemFields.MEDIA_SOURCE_COUNT
				),
				recursive = true,
				enableTotalRecordCount = false,
			)
			Result.success(result.content.items)
		} catch (e: ApiClientException) {
			Timber.w("Failed to search for items: ${e.message}")
			Result.failure(e)
		}
	}
}
