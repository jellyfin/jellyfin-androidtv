package org.jellyfin.androidtv.ui.presentation

import androidx.leanback.widget.RowHeaderPresenter

class CustomRowHeaderPresenter : RowHeaderPresenter() {
	override fun onSelectLevelChanged(holder: ViewHolder) {
		// Do nothing - this keeps headers opaque
	}
}
