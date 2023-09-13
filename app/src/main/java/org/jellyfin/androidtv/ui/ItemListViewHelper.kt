package org.jellyfin.androidtv.ui

import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.ItemFields
import org.koin.java.KoinJavaComponent

fun ItemListView.refresh() {
	val api by KoinJavaComponent.inject<ApiClient>(ApiClient::class.java)

	findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
		val response by api.itemsApi.getItemsByUserId(
			ids = mItemIds,
			fields = setOf(
				ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
				ItemFields.OVERVIEW,
				ItemFields.ITEM_COUNTS,
				ItemFields.DISPLAY_PREFERENCES_ID,
				ItemFields.CHILD_COUNT,
				ItemFields.MEDIA_SOURCES,
			)
		)

		response.items?.forEachIndexed { index, item ->
			val view = mList.getChildAt(index)
			if (view is ItemRowView) view.setItem(item, index)
		}
	}
}
