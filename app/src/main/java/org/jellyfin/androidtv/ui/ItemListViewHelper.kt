package org.jellyfin.androidtv.ui

import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.koin.java.KoinJavaComponent

fun ItemListView.refresh() {
	val api by KoinJavaComponent.inject<ApiClient>(ApiClient::class.java)

	findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
		val response by api.itemsApi.getItems(
			ids = mItemIds,
			fields = ItemRepository.itemFields
		)

		response.items?.forEachIndexed { index, item ->
			val view = mList.getChildAt(index)
			if (view is ItemRowView) view.setItem(item, index)
		}
	}
}
