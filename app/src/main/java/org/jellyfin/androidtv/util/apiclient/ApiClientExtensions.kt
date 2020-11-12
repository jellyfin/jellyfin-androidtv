package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.androidtv.TvApp
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.UserDto
import org.jellyfin.apiclient.model.querying.ItemsResult
import org.jellyfin.apiclient.model.querying.NextUpQuery
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
 * Coroutine capable version of the "getPublicUsers" function
 */
suspend fun ApiClient.getPublicUsers(): Array<UserDto>? = suspendCoroutine { continuation ->
	GetPublicUsersAsync(object : Response<Array<UserDto>>() {
		override fun onResponse(response: Array<UserDto>?) = continuation.resume(response!!)
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

suspend fun <T : Any> callApi(init: (callback: Response<T>) -> Unit): T = suspendCoroutine { continuation ->
	init(object : Response<T>() {
		override fun onResponse(response: T) = continuation.resumeWith(Result.success(response))
		override fun onError(exception: Exception) = continuation.resumeWith(Result.failure(exception))
	})
}
