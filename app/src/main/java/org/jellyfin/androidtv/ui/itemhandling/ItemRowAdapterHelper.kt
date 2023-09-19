package org.jellyfin.androidtv.ui.itemhandling

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import timber.log.Timber
import kotlin.math.min

fun <T : Any> ItemRowAdapter.setItems(
	items: Array<T>,
	transform: (T, Int) -> BaseRowItem?,
) {
	Timber.d("Creating items from $itemsLoaded existing and ${items.size} new, adapter size is ${size()}")

	val allItems = buildList {
		// Add current items before loaded items
		repeat(itemsLoaded) {
			add(this@setItems.get(it))
		}

		// Add loaded items
		val mappedItems = items.mapIndexedNotNull { index, item ->
			transform(item, itemsLoaded + index)
		}
		mappedItems.forEach { add(it) }

		// Add current items after loaded items
		repeat(min(totalItems, size()) - itemsLoaded - mappedItems.size) {
			add(this@setItems.get(it + itemsLoaded + mappedItems.size))
		}
	}

	replaceAll(allItems)
	itemsLoaded = allItems.size
}

fun ItemRowAdapter.retrieveResumeItems(api: ApiClient, query: GetResumeItemsRequest) {
	ProcessLifecycleOwner.get().lifecycleScope.launch {
		val response by api.itemsApi.getResumeItems(query)

		setItems(
			items = response.items.orEmpty().toTypedArray(),
			transform = { item, i -> BaseRowItem(item, i, preferParentThumb, isStaticHeight) }
		)

		if (response.items.isNullOrEmpty()) removeRow()
	}
}
