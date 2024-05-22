package org.jellyfin.androidtv.ui.itemhandling

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
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
			transform = { item, i ->
				BaseItemDtoBaseRowItem(
					i,
					item,
					preferParentThumb,
					isStaticHeight
				)
			}
		)

		if (response.items.isNullOrEmpty()) removeRow()
	}
}

@JvmOverloads
fun ItemRowAdapter.refreshItem(
	api: ApiClient,
	lifecycleOwner: LifecycleOwner,
	currentBaseRowItem: BaseRowItem,
	callback: () -> Unit = {}
) {
	if (currentBaseRowItem !is BaseItemDtoBaseRowItem) return
	val currentBaseItem = currentBaseRowItem.baseItem ?: return

	lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
		runCatching {
			api.userLibraryApi.getItem(itemId = currentBaseItem.id).content
		}.fold(
			onSuccess = { refreshedBaseItem ->
				withContext(Dispatchers.Main) {
					set(
						index = indexOf(currentBaseRowItem),
						element = BaseItemDtoBaseRowItem(
							index = currentBaseRowItem.index,
							item = refreshedBaseItem,
							preferParentThumb = currentBaseRowItem.preferParentThumb,
							staticHeight = currentBaseRowItem.staticHeight,
							selectAction = currentBaseRowItem.selectAction,
							preferSeriesPoster = currentBaseRowItem.preferSeriesPoster
						)
					)
				}
			},
			onFailure = { err ->
				if (err is InvalidStatusException && err.status == 404) withContext(Dispatchers.Main) {
					remove(currentBaseRowItem)
				} else Timber.e(err, "Failed to refresh item")
			}
		)

		callback()
	}
}
