package org.jellyfin.androidtv.ui.presentation

import androidx.leanback.widget.RowHeaderPresenter

class CustomRowHeaderPresenter(private val homeHeaderEnabled: Boolean = false) : RowHeaderPresenter() {
	override fun onSelectLevelChanged(holder: ViewHolder) {
		// Do nothing - this keeps headers opaque
		// Unless on home screen and the item preview is enabled - add headers back manually
		if (homeHeaderEnabled) holder.view.alpha = 0f
	}
}
