package org.jellyfin.androidtv.ui.itemhandling

import timber.log.Timber

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
		repeat(size() - itemsLoaded - mappedItems.size) {
			add(this@setItems.get(it + itemsLoaded + mappedItems.size))
		}
	}

	replaceAll(allItems)
	itemsLoaded = allItems.size
}
