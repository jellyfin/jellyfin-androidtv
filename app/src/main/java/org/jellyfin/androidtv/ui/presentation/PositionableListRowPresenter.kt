package org.jellyfin.androidtv.ui.presentation

import android.view.KeyEvent
import androidx.leanback.widget.RowPresenter
import timber.log.Timber

class PositionableListRowPresenter : CustomListRowPresenter {
	private var viewHolder: ViewHolder? = null
	private val trapFocusAtStart: Boolean

	constructor() : this(padding = null, trapFocusAtStart = false)
	constructor(padding: Int?) : this(padding, trapFocusAtStart = false)
	constructor(padding: Int? = null, trapFocusAtStart: Boolean = false) : super(padding) {
		this.trapFocusAtStart = trapFocusAtStart
	}

	init {
		shadowEnabled = false
	}

	override fun isUsingDefaultShadow() = false

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit

	override fun onBindRowViewHolder(holder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(holder, item)
		if (holder !is ViewHolder) return

		viewHolder = holder
		if (trapFocusAtStart) {
			// Prevent focus from escaping the grid at the left boundary so the user
			// stays inside the popup (channel changer / chapter selector).
			holder.gridView?.setOnKeyInterceptListener { event ->
				event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
					(holder.gridView?.selectedPosition ?: -1) <= 0
			}
		}
	}

	var position: Int
		get() = viewHolder?.gridView?.selectedPosition ?: -1
		set(value) {
			Timber.d("Setting position to $value")
			viewHolder?.gridView?.selectedPosition = value
		}
}
