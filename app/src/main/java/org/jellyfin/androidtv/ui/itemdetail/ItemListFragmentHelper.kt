package org.jellyfin.androidtv.ui.itemdetail

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.koin.android.ext.android.inject
import java.util.UUID

private val itemFields = setOf(
	ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
	ItemFields.OVERVIEW,
	ItemFields.ITEM_COUNTS,
	ItemFields.DISPLAY_PREFERENCES_ID,
	ItemFields.CHILD_COUNT,
	ItemFields.GENRES,
	ItemFields.CHAPTERS,
)

fun ItemListFragment.loadItem(itemId: UUID) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val item by api.userLibraryApi.getItem(itemId)
		if (isActive) setBaseItem(item)
	}
}

fun MusicFavoritesListFragment.getFavoritePlaylist(
	parentId: UUID?,
	callback: (items: List<BaseItemDto>) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result by api.itemsApi.getItems(
			parentId = parentId,
			includeItemTypes = setOf(BaseItemKind.AUDIO),
			recursive = true,
			filters = setOf(org.jellyfin.sdk.model.api.ItemFilter.IS_FAVORITE_OR_LIKES),
			sortBy = setOf(ItemSortBy.RANDOM),
			limit = 100,
			fields = itemFields,
		)

		callback(result.items)
	}
}

fun ItemListFragment.getPlaylist(
	item: BaseItemDto,
	callback: (items: List<BaseItemDto>) -> Unit
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val result by when {
			item.type == BaseItemKind.PLAYLIST -> api.playlistsApi.getPlaylistItems(
				playlistId = item.id,
				limit = 150,
				fields = itemFields,
			)

			else -> api.itemsApi.getItems(
				parentId = item.id,
				includeItemTypes = setOf(BaseItemKind.AUDIO),
				recursive = true,
				sortBy = setOf(ItemSortBy.SORT_NAME),
				limit = 200,
				fields = itemFields,
			)
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
