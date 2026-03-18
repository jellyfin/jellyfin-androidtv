package org.jellyfin.androidtv.ui.presentation

import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback

/**
 * A leanback ObjectAdapter using a Kotlin list as backend. Implements Iterable to allow collection
 * operations. And uses generics for strong typing. Uses a MutableList as internal structure.
 */
open class MutableObjectAdapter<T : Any> : ObjectAdapter, Iterable<T> {
	private val data = mutableListOf<T>()
	private val hiddenItems = mutableMapOf<Int, T>()

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
	fun getHiddenItemsCount(): Int = hiddenItems.size
	fun getHiddenItems(): List<T> = hiddenItems.values.toList()

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

	fun replaceAll(
		items: List<T>,
		areItemsTheSame: (old: T, new: T) -> Boolean = { old, new -> old == new },
		areContentsTheSame: (old: T, new: T) -> Boolean = { old, new -> old == new },
	) {
		val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
			override fun getOldListSize(): Int = data.size
			override fun getNewListSize(): Int = items.size

			override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
				areItemsTheSame(data[oldItemPosition], items[newItemPosition])

			override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
				areContentsTheSame(data[oldItemPosition], items[newItemPosition])
		})

		data.clear()
		hiddenItems.clear()
		data.addAll(items)

		diff.dispatchUpdatesTo(object : ListUpdateCallback {
			override fun onInserted(position: Int, count: Int) = notifyItemRangeInserted(position, count)
			override fun onRemoved(position: Int, count: Int) = notifyItemRangeRemoved(position, count)
			override fun onMoved(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)
			override fun onChanged(position: Int, count: Int, payload: Any?) = notifyItemRangeChanged(position, count, payload)
		})
	}

	fun clear() {
		val size = data.size
		if (size == 0) return

		notifyItemRangeRemoved(0, size)
		data.clear()
		hiddenItems.clear()
	}

	fun remove(element: T): Boolean {
		val index = indexOf(element)
		if (index == -1) return false
		return removeAt(index, 1)
	}

	fun removeAt(index: Int, length: Int = 1): Boolean {
		if (index < 0 || index >= data.size) return false

		data.subList(index, index + length).clear()
		notifyItemRangeRemoved(index, length)

		return true
	}

	fun hide(element: T): Boolean {
		val index = indexOf(element)
		if (index == -1) return false
		return hideAt(index)
	}

	fun hideAt(index: Int): Boolean {
		if (index < 0 || index >= data.size) return false

		hiddenItems[index] = data[index]
		if(!removeAt(index)) {
			hiddenItems.remove(index)
			return false
		}

		return true
	}

	fun show(element: T): Boolean {
		val index = hiddenItems.entries.find { it.value == element }?.key ?: return false
		return showAt(index)
	}

	fun showAt(index: Int): Boolean {
		if (index < 0 || index >= data.size) return false

		hiddenItems.remove(index)?.let { add(index, it) }

		return true
	}

	fun clearHidden() {
		hiddenItems.clear()
	}

	fun indexOf(item: T) = data.indexOf(item)
}
