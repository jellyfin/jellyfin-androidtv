package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import timber.log.Timber

object BrowsingUtils {
	@JvmStatic
	fun getRandomItem(api: ApiClient, lifecycle: LifecycleOwner, library: BaseItemDto, type: BaseItemKind, callback: (item: BaseItemDto?) -> Unit) {
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
}
