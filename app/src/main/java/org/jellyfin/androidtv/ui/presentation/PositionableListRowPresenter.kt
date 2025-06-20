package org.jellyfin.androidtv.ui.presentation

import androidx.leanback.widget.RowPresenter
import timber.log.Timber

class PositionableListRowPresenter : CustomListRowPresenter {
	private var viewHolder: ViewHolder? = null

	constructor() : super()
	constructor(padding: Int?) : super(padding)

	init {
		shadowEnabled = false
	}

	override fun isUsingDefaultShadow() = false

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit

	override fun onBindRowViewHolder(holder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(holder, item)
		if (holder !is ViewHolder) return

		viewHolder = holder
	}

	var position: Int
		get() = viewHolder?.gridView?.selectedPosition ?: -1
		set(value) {
			Timber.d("Setting position to $value")
			viewHolder?.gridView?.selectedPosition = value
		}
}
