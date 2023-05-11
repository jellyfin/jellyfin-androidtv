package org.jellyfin.androidtv.util.apiclient

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull

/**
 * A sealed class representing the state of an asynchronous operation.
 */
sealed class AsyncState<out T> {
    object Loading : AsyncState<Nothing>()
    data class Success<out T>(val data: T) : AsyncState<T>()
    data class Error(val throwable: Throwable) : AsyncState<Nothing>()
}

/**
 * Returns the first [AsyncState.Success] value emitted by the flow, or null if the flow was not
 * successful.  Useful for quickly getting the results without having to handle the
 * Loading and Error states.
 */
suspend inline fun <reified T> Flow<AsyncState<T>>.waitForSuccess(): T? {
	return filterIsInstance<AsyncState.Success<T>>()
		.firstOrNull()?.data
}

