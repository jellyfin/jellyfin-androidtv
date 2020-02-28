package org.jellyfin.androidtv.details

import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector

class StateObjectAdapter<T> : ObjectAdapter where T : Any {
	constructor() : super()
	constructor(presenter: Presenter) : super(presenter)
	constructor(presenterSelector: PresenterSelector) : super(presenterSelector)

	private val items = mutableListOf<ItemHolder<T>>()

	override fun size() = items.count { it.visible }
	override fun get(position: Int) = items.filter { it.visible }[position].item

	fun add(item: T) = items.add(ItemHolder(item))

	fun setVisibility(item: T, visible: Boolean) {
		items.forEachIndexed { index, holder ->
			if (holder.item == item && holder.visible != visible) {
				holder.visible = visible
			}
		}

		notifyChanged()
	}

	private data class ItemHolder<T>(val item: T, var visible: Boolean = true)
}
