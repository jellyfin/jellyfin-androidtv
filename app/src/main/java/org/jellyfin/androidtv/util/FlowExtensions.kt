package org.jellyfin.androidtv.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Collect all items emitted from flow of type T to a List<T> and return it as a LiveData instance.
 * The LiveData is updated for each item emitted from the flow.
 *
 * To remove duplicate entries use the [distinctUntilChanged] or [distinctUntilChangedBy] functions.
 */
fun <T> Flow<T>.asLiveDataCollection(
	context: CoroutineContext = EmptyCoroutineContext
): LiveData<List<T>> {
	val list = mutableListOf<T>()
	val liveData = MediatorLiveData<List<T>>()

	liveData.addSource(asLiveData(context)) {
		list.add(it)
		liveData.value = list
	}

	return liveData
}
