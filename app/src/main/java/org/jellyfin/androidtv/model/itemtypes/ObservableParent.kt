package org.jellyfin.androidtv.model.itemtypes

import kotlin.reflect.KProperty

abstract class ObservableParent {
	private val changeListeners = mutableListOf<() -> Unit>()

	fun addChangeListener(listener: () -> Unit) { changeListeners.add(listener) }
	fun removeChangeListener(listener: () -> Unit) { changeListeners.remove(listener) }

	protected fun <T> observer(property: KProperty<*>, oldValue: T, newValue: T) {
		if (oldValue == newValue) return

		changeListeners.forEach { it.invoke() }
	}
}
