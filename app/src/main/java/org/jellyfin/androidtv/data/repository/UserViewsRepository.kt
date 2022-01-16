package org.jellyfin.androidtv.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import org.jellyfin.apiclient.model.entities.CollectionType
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemDto

interface UserViewsRepository {
	val views: LiveData<Collection<BaseItemDto>>

	fun isSupported(collectionType: String): Boolean
	fun allowViewSelection(collectionType: String): Boolean
}

class UserViewsRepositoryImpl(
	private val api: ApiClient,
) : UserViewsRepository {
	override val views: LiveData<Collection<BaseItemDto>> = liveData {
		val views by api.userViewsApi.getUserViews()
		val filteredViews = views.items
			.orEmpty()
			.filter { isSupported(it.collectionType.orEmpty()) }
		emit(filteredViews)
	}

	override fun isSupported(collectionType: String) = collectionType !in unsupportedCollectionTypes
	override fun allowViewSelection(collectionType: String) = collectionType != CollectionType.Music

	private companion object {
		private val unsupportedCollectionTypes = arrayOf(
			CollectionType.Books,
			CollectionType.Games,
			CollectionType.Folders
		)
	}
}
