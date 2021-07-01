package org.jellyfin.androidtv.ui.presentation

import androidx.leanback.widget.RowHeaderPresenter

class CustomRowHeaderPresenter(private val homeHeaderEnabled: Boolean) : RowHeaderPresenter() {
	constructor() : this(false)

	override fun onSelectLevelChanged(holder: ViewHolder) {
		// Do nothing - this keeps headers opaque
		// Unless on home screen - add headers back manually
		if (homeHeaderEnabled) holder.view.alpha = 0f
	}
}
