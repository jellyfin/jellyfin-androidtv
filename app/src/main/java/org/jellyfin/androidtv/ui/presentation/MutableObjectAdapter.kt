package org.jellyfin.androidtv.ui.presentation

import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector

/**
 * A leanback ObjectAdapter using a Kotlin list as backend. Implements Iterable to allow collection
 * operations. And uses generics for strong typing. Uses a MutableList as internal stucture.
 */
class MutableObjectAdapter<T : Any> : ObjectAdapter, Iterable<T> {
	private val data = mutableListOf<T>()

	// Constructors
	constructor(presenterSelector: PresenterSelector) : super(presenterSelector)
	constructor(presenter: Presenter) : super(presenter)
	constructor() : super()

	// ObjectAdapter
	override fun size(): Int = data.size
	override fun get(index: Int): T? = data.getOrNull(index)

	// Iterable
	override fun iterator(): Iterator<T> = data.iterator()

	// Custom
	fun add(element: T) {
		data.add(element)
		notifyItemRangeInserted(data.size - 1, 1)
	}

	fun add(index: Int, element: T) {
		data.add(index, element)
		notifyItemRangeInserted(index, 1)
	}

	fun set(index: Int, element: T) {
		data.set(index, element)
		notifyItemRangeChanged(index, 1)
	}

	fun clear() {
		val size = data.size
		if (size == 0) return

		notifyItemRangeRemoved(0, size)
		data.clear()
	}

	fun remove(element: T): Boolean {
		val removed = data.remove(element)
		if (removed) notifyChanged()
		return removed
	}
}
