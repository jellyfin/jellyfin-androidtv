package org.jellyfin.playback.core.element

import java.util.concurrent.ConcurrentHashMap

/**
 * Container to hold elements identified with an [ElementKey].
 */
open class ElementsContainer {
	private val elements = ConcurrentHashMap<ElementKey<*>, Any?>()

	fun <T : Any> get(key: ElementKey<T>): T = getOrNull(key)
		?: error("No element found for key $key.")

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> getOrNull(key: ElementKey<T>): T? = elements[key] as T?

	operator fun <T : Any> contains(key: ElementKey<T>): Boolean = elements.containsKey(key)

	fun <T : Any> put(key: ElementKey<T>, value: T) {
		elements[key] = value
	}

	fun <T : Any> remove(key: ElementKey<T>) {
		elements.remove(key)
	}
}
