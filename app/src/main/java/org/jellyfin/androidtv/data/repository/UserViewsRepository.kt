package org.jellyfin.androidtv.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemDto

interface UserViewsRepository {
	val views: LiveData<Collection<BaseItemDto>>
}

class UserViewsRepositoryImpl(
	private val api: ApiClient,
) : UserViewsRepository {
	override val views: LiveData<Collection<BaseItemDto>> = liveData {
		val views by api.userViewsApi.getUserViews()
		val filteredViews = views.items
			.orEmpty()
			.filterNot { it.collectionType in ItemRowAdapter.ignoredCollectionTypes }
		emit(filteredViews)
	}
}
