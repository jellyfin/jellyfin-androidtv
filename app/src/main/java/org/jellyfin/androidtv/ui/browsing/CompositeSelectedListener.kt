package org.jellyfin.androidtv.ui.browsing

import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter

class CompositeSelectedListener : OnItemViewSelectedListener {
	private val listeners = mutableListOf<OnItemViewSelectedListener>()

	fun registerListener(listener: OnItemViewSelectedListener) = listeners.add(listener)

	override fun onItemSelected(
		itemViewHolder: Presenter.ViewHolder?,
		item: Any?,
		rowViewHolder: RowPresenter.ViewHolder?,
		row: Row?,
	) {
		for (listener in listeners) {
			listener.onItemSelected(itemViewHolder, item, rowViewHolder, row)
		}
	}

	fun removeListeners() = listeners.clear()
}
