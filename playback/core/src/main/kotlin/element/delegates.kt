package org.jellyfin.playback.core.element

import kotlinx.coroutines.flow.Flow
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Delegate for an optional element.
 */
fun <T : Any> element(
	key: ElementKey<T>
) = object : ReadWriteProperty<ElementsContainer, T?> {
	override fun getValue(thisRef: ElementsContainer, property: KProperty<*>): T? =
		thisRef.getOrNull(key)

	override fun setValue(thisRef: ElementsContainer, property: KProperty<*>, value: T?) {
		if (value == null) thisRef.remove(key)
		else thisRef.put(key, value)
	}
}

/**
 * Delegate for an required element.
 */
fun <T : Any> requiredElement(
	key: ElementKey<T>,
	computeDefault: () -> T,
) = object : ReadWriteProperty<ElementsContainer, T> {
	override fun getValue(thisRef: ElementsContainer, property: KProperty<*>): T {
		val value = thisRef.getOrNull(key)
		if (value != null) return value

		val default = computeDefault()
		thisRef.put(key, default)
		return default
	}

	override fun setValue(thisRef: ElementsContainer, property: KProperty<*>, value: T) {
		thisRef.put(key, value)
	}
}

/**
 * Delegate for the flow of an element.
 */
fun <T : Any> elementFlow(
	key: ElementKey<T>,
) = ReadOnlyProperty<ElementsContainer, Flow<T?>> { thisRef, _ -> thisRef.getFlow(key) }
