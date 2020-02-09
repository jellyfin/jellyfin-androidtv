package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.model.itemtypes.FIELDS_REQUIRED_FOR_LIFT
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.UserItemDataDto
import org.jellyfin.apiclient.model.querying.ItemsResult
import org.jellyfin.apiclient.model.querying.NextUpQuery
import org.jellyfin.apiclient.model.querying.SimilarItemsQuery
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Coroutine capable version of the "getNextUpEpisodes" function
 */
suspend fun ApiClient.getNextUpEpisodes(query: NextUpQuery): ItemsResult? = suspendCoroutine { continuation ->
	GetNextUpEpisodesAsync(query, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) = continuation.resume(response!!)
		override fun onError(exception: Exception?) = continuation.resume(null)
	})
}

/**
 * Coroutine capable version of the "getUserViews" function
 * Uses the userId of the currently signed in user
 */
suspend fun ApiClient.getUserViews(): ItemsResult? = suspendCoroutine { continuation ->
	GetUserViews(currentUserId, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) = continuation.resume(response!!)
		override fun onError(exception: Exception?) = continuation.resume(null)
	})
}

/**
 * Adds a coroutine capable version of the "GetItem" function
 * Uses the userId of the currently signed in user
 */
suspend fun ApiClient.getItem(id: String): BaseItemDto? = suspendCoroutine { continuation ->
	GetItemAsync(id, TvApp.getApplication().currentUser.id, object : Response<BaseItemDto>() {
		override fun onResponse(response: BaseItemDto?) = continuation.resume(response!!)
		override fun onError(exception: Exception?) = continuation.resume(null)
	})
}

suspend fun ApiClient.markPlayed(itemId: String, userId: String, datePlayed: Date?) : UserItemDataDto? = suspendCoroutine { continuation ->
	MarkPlayedAsync(itemId, userId, datePlayed, object : Response<UserItemDataDto>() {
		override fun onResponse(response: UserItemDataDto?) { continuation.resume(response!!) }
		override fun onError(exception: Exception?) { continuation.resume(null) }
	})
}

suspend fun ApiClient.markUnplayed(itemId: String, userId: String) : UserItemDataDto? = suspendCoroutine { continuation ->
	MarkUnplayedAsync(itemId, userId, object : Response<UserItemDataDto>() {
		override fun onResponse(response: UserItemDataDto?) { continuation.resume(response!!) }
		override fun onError(exception: Exception?) { continuation.resume(null) }
	})
}

suspend fun ApiClient.updateFavoriteStatus(itemId: String, userId: String, isFavorite: Boolean) : UserItemDataDto? = suspendCoroutine { continuation ->
	UpdateFavoriteStatusAsync(itemId, userId, isFavorite, object : Response<UserItemDataDto>() {
		override fun onResponse(response: UserItemDataDto?) { continuation.resume(response!!) }
		override fun onError(exception: Exception?) { continuation.resume(null) }
	})
}

suspend fun ApiClient.getSimilarItems(item: BaseItem, limit: Int = 25): List<BaseItem>? = suspendCoroutine { continuation ->
	val query = SimilarItemsQuery().apply {
		id = item.id
		userId = TvApp.getApplication().currentUser.id
		this.limit = limit
		fields = FIELDS_REQUIRED_FOR_LIFT
	}

	GetSimilarItems(query, object : Response<ItemsResult>() {
		override fun onResponse(response: ItemsResult?) {
			continuation.resume(response?.items.orEmpty().map { baseItemDto -> baseItemDto.liftToNewFormat() })
		}

		override fun onError(exception: java.lang.Exception?) {
			continuation.resume(null)
		}
	})
}
