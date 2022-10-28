package org.jellyfin.androidtv.ui.browsing

import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter

class CompositeClickedListener : OnItemViewClickedListener {
	private val listeners = mutableListOf<OnItemViewClickedListener>()

	fun registerListener(listener: OnItemViewClickedListener) = listeners.add(listener)

	override fun onItemClicked(
		itemViewHolder: Presenter.ViewHolder?,
		item: Any?,
		rowViewHolder: RowPresenter.ViewHolder?,
		row: Row?,
	) {
		for (listener in listeners) {
			listener.onItemClicked(itemViewHolder, item, rowViewHolder, row)
		}
	}

	fun removeListeners() = listeners.clear()
}
