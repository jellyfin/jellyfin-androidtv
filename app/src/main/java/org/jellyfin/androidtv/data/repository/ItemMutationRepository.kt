package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userDataApi
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.UserItemDataDto
import java.time.Instant

interface ItemMutationRepository {
	suspend fun setFavorite(item: UUID, favorite: Boolean): UserItemDataDto
	suspend fun setPlayed(item: UUID, played: Boolean): UserItemDataDto
}

class ItemMutationRepositoryImpl(
	private val api: ApiClient,
	private val dataRefreshService: DataRefreshService,
) : ItemMutationRepository {
	override suspend fun setFavorite(item: UUID, favorite: Boolean): UserItemDataDto {
		val response by when {
			favorite -> withContext(Dispatchers.IO) { api.userDataApi.markFavoriteItem(itemId = item) }
			else -> withContext(Dispatchers.IO) { api.userDataApi.unmarkFavoriteItem(itemId = item) }
		}

		dataRefreshService.lastFavoriteUpdate = Instant.now()
		return response
	}

	override suspend fun setPlayed(item: UUID, played: Boolean): UserItemDataDto {
		val response by when {
			played -> withContext(Dispatchers.IO) { api.userDataApi.markPlayedItem(itemId = item) }
			else -> withContext(Dispatchers.IO) { api.userDataApi.markUnplayedItem(itemId = item) }
		}

		return response
	}
}
