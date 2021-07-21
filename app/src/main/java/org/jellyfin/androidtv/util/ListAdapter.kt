package org.jellyfin.androidtv.util

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class ListAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
	private var _items: List<T> = emptyList()
	var items
		get() = _items
		set(newItems) {
			val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
				override fun getOldListSize(): Int = _items.size
				override fun getNewListSize(): Int = newItems.size

				override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
					areItemsTheSame(_items[oldItemPosition], newItems[newItemPosition])

				override fun areContentsTheSame(
					oldItemPosition: Int,
					newItemPosition: Int,
				): Boolean =
					areContentsTheSame(_items[oldItemPosition], newItems[newItemPosition])
			})

			_items = newItems

			diff.dispatchUpdatesTo(this)
		}

	protected open fun areItemsTheSame(old: T, new: T): Boolean = areContentsTheSame(old, new)
	protected open fun areContentsTheSame(old: T, new: T): Boolean = old == new

	override fun getItemCount(): Int = _items.size
	override fun onBindViewHolder(holder: VH, position: Int) = onBindViewHolder(holder, _items[position])
	protected abstract fun onBindViewHolder(holder: VH, item: T)
}
