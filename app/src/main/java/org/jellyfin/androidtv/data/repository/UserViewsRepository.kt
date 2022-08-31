package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.constant.CollectionType

interface UserViewsRepository {
	val views: Flow<Collection<BaseItemDto>>

	fun isSupported(collectionType: String?): Boolean
	fun allowViewSelection(collectionType: String?): Boolean
	fun allowGridView(collectionType: String?): Boolean
}

class UserViewsRepositoryImpl(
	private val api: ApiClient,
) : UserViewsRepository {
	override val views = flow {
		val views by api.userViewsApi.getUserViews()
		val filteredViews = views.items
			.orEmpty()
			.filter { isSupported(it.collectionType) }
		emit(filteredViews)
	}

	override fun isSupported(collectionType: String?) = collectionType !in unsupportedCollectionTypes
	override fun allowViewSelection(collectionType: String?) = collectionType !in disallowViewSelectionCollectionTypes
	override fun allowGridView(collectionType: String?) = collectionType !in disallowGridViewCollectionTypes

	private companion object {
		private val unsupportedCollectionTypes = arrayOf(
			CollectionType.Books,
			CollectionType.Folders
		)

		private val disallowViewSelectionCollectionTypes = arrayOf(
			CollectionType.LiveTv,
			CollectionType.Music,
			CollectionType.Photos,
		)

		private val disallowGridViewCollectionTypes = arrayOf(
			CollectionType.LiveTv,
			CollectionType.Music
		)
	}
}
