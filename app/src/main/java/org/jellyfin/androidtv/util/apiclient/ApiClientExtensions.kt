package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.EmptyResponse
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.querying.ItemsResult
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
suspend fun ApiClient.getItem(id: String, userId: UUID): BaseItemDto? = suspendCoroutine { continuation ->
	GetItemAsync(id,  userId.toString(), object : Response<BaseItemDto>() {
		override fun onResponse(response: BaseItemDto?) = continuation.resume(response!!)
		override fun onError(exception: Exception?) = continuation.resume(null)
	})
}

suspend fun <T : Any?> callApi(init: (callback: Response<T>) -> Unit): T = suspendCoroutine { continuation ->
	init(object : Response<T>() {
		override fun onResponse(response: T) = continuation.resumeWith(Result.success(response))
		override fun onError(exception: Exception) = continuation.resumeWith(Result.failure(exception))
	})
}

suspend fun callApiEmpty(init: (callback: EmptyResponse) -> Unit): Unit = suspendCoroutine { continuation ->
	init(object : EmptyResponse() {
		override fun onResponse() = continuation.resumeWith(Result.success(Unit))
		override fun onError(exception: Exception) = continuation.resumeWith(Result.failure(exception))
	})
}
