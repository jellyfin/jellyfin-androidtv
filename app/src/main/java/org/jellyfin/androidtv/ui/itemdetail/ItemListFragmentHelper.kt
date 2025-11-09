package org.jellyfin.androidtv.ui.itemdetail

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.model.PlaylistPaginationState
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.android.ext.android.inject
import java.util.UUID

fun ItemListFragment.loadItem(itemId: UUID) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val item = withContext(Dispatchers.IO) {
			api.userLibraryApi.getItem(itemId).content
		}
		if (isActive) setBaseItem(item)
	}
}

fun MusicFavoritesListFragment.getFavoritePlaylist(
	parentId: UUID?,
	callback: (items: List<BaseItemDto>) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result = withContext(Dispatchers.IO) {
			api.itemsApi.getItems(
				parentId = parentId,
				includeItemTypes = setOf(BaseItemKind.AUDIO),
				recursive = true,
				filters = setOf(org.jellyfin.sdk.model.api.ItemFilter.IS_FAVORITE_OR_LIKES),
				sortBy = setOf(ItemSortBy.RANDOM),
				limit = 100,
				fields = ItemRepository.itemFields,
			).content
		}

		callback(result.items)
	}
}

data class PlaylistResult(
	val items: List<BaseItemDto>,
	val totalItems: Int,
	val startIndex: Int
)

fun ItemListFragment.getPlaylist(
	item: BaseItemDto,
	callback: (items: List<BaseItemDto>) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result = withContext(Dispatchers.IO) {
			when {
				item.type == BaseItemKind.PLAYLIST -> api.playlistsApi.getPlaylistItems(
					playlistId = item.id,
					limit = 150,
					fields = ItemRepository.itemFields,
				).content

				else -> api.itemsApi.getItems(
					parentId = item.id,
					includeItemTypes = setOf(BaseItemKind.AUDIO),
					recursive = true,
					sortBy = setOf(ItemSortBy.SORT_NAME),
					limit = 200,
					fields = ItemRepository.itemFields,
				).content
			}
		}

		callback(result.items)
	}
}

fun ItemListFragment.getPlaylistPaginated(
	item: BaseItemDto,
	paginationState: PlaylistPaginationState,
	callback: (result: PlaylistResult) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result = withContext(Dispatchers.IO) {
			when {
				item.type == BaseItemKind.PLAYLIST -> {
					val playlistResult = api.playlistsApi.getPlaylistItems(
						playlistId = item.id,
						startIndex = paginationState.startIndex,
						limit = paginationState.pageSize,
						fields = ItemRepository.itemFields,
					).content

					PlaylistResult(
						items = playlistResult.items,
						totalItems = playlistResult.totalRecordCount ?: playlistResult.items.size,
						startIndex = paginationState.startIndex
					)
				}

				else -> {
					val itemsResult = api.itemsApi.getItems(
						parentId = item.id,
						includeItemTypes = setOf(BaseItemKind.AUDIO),
						recursive = true,
						sortBy = setOf(ItemSortBy.SORT_NAME),
						limit = 200,
						fields = ItemRepository.itemFields,
					).content

					PlaylistResult(
						items = itemsResult.items,
						totalItems = itemsResult.totalRecordCount ?: itemsResult.items.size,
						startIndex = 0
					)
				}
			}
		}

		callback(result)
	}
}

fun ItemListFragment.toggleFavorite(item: BaseItemDto, callback: (item: BaseItemDto) -> Unit) {
	val itemMutationRepository by inject<ItemMutationRepository>()

	lifecycleScope.launch {
		val userData = itemMutationRepository.setFavorite(
			item = item.id,
			favorite = !(item.userData?.isFavorite ?: false)
		)
		callback(item.copy(userData = userData))
	}
}

fun ItemListFragment.findFirstUnwatchedItemInPlaylist(
	playlistId: UUID,
	callback: (firstUnwatchedItem: BaseItemDto?) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result = withContext(Dispatchers.IO) {
			runCatching {
				api.itemsApi.getItems(
					parentId = playlistId,
					recursive = true,
					filters = setOf(ItemFilter.IS_UNPLAYED),
					sortBy = setOf(ItemSortBy.SORT_NAME),
					sortOrder = setOf(SortOrder.ASCENDING),
					limit = 1, // Only need the first result
					fields = ItemRepository.itemFields
				).content.items?.firstOrNull()
			}.getOrNull()
		}
		callback(result)
	}
}
