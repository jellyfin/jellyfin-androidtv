package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType

interface UserViewsRepository {
	val views: Flow<Collection<BaseItemDto>>

	fun isSupported(collectionType: CollectionType?): Boolean
	fun allowViewSelection(collectionType: CollectionType?): Boolean
	fun allowGridView(collectionType: CollectionType?): Boolean
}

class UserViewsRepositoryImpl(
	private val api: ApiClient,
) : UserViewsRepository {
	override val views = flow {
		val views by api.userViewsApi.getUserViews()
		val filteredViews = views.items
			.filter { isSupported(it.collectionType) }
		emit(filteredViews)
	}.flowOn(Dispatchers.IO)

	override fun isSupported(collectionType: CollectionType?) = collectionType !in unsupportedCollectionTypes
	override fun allowViewSelection(collectionType: CollectionType?) = collectionType !in disallowViewSelectionCollectionTypes
	override fun allowGridView(collectionType: CollectionType?) = collectionType !in disallowGridViewCollectionTypes

	private companion object {
		private val unsupportedCollectionTypes = arrayOf(
			CollectionType.BOOKS,
			CollectionType.FOLDERS
		)

		private val disallowViewSelectionCollectionTypes = arrayOf(
			CollectionType.LIVETV,
			CollectionType.MUSIC,
			CollectionType.PHOTOS,
		)

		private val disallowGridViewCollectionTypes = arrayOf(
			CollectionType.LIVETV,
			CollectionType.MUSIC
		)
	}
}
