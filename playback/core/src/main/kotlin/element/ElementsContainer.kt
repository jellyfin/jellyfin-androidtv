package org.jellyfin.playback.core.element

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/**
 * Container to hold elements identified with an [ElementKey].
 */
open class ElementsContainer {
	private val elements = ConcurrentHashMap<ElementKey<*>, Any?>()
	private val updateFlow = MutableSharedFlow<ElementKey<*>>(
		replay = 1,
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST,
	)

	fun <T : Any> get(key: ElementKey<T>): T = getOrNull(key)
		?: error("No element found for key $key.")

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getOrNull(key: ElementKey<T>): T? = elements[key] as T?

	operator fun <T : Any> contains(key: ElementKey<T>): Boolean = elements.containsKey(key)

	fun <T : Any> put(key: ElementKey<T>, value: T) {
		elements[key] = value
		updateFlow.tryEmit(key)
	}

	fun <T : Any> remove(key: ElementKey<T>) {
		elements.remove(key)
		updateFlow.tryEmit(key)
	}

	fun <T : Any> getFlow(key: ElementKey<T>): Flow<T?> {
		return updateFlow
			.map { getOrNull(key) }
			.distinctUntilChanged()
	}
}
