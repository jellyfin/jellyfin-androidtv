package org.jellyfin.androidtv.ui.itemdetail

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.playlistApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.koin.android.ext.android.inject
import java.util.UUID

fun ItemListFragment.loadItem(itemId: UUID) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val item = withContext(Dispatchers.IO) {
			api.libraryApi.getItem(itemId).content
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
			api.libraryApi.getItems(
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

fun ItemListFragment.getPlaylist(
	item: BaseItemDto,
	callback: (items: List<BaseItemDto>) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result = withContext(Dispatchers.IO) {
			when {
				item.type == BaseItemKind.PLAYLIST -> api.playlistApi.getPlaylistItems(
					playlistId = item.id,
					limit = 150,
					fields = ItemRepository.itemFields,
				).content

				else -> api.libraryApi.getItems(
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
