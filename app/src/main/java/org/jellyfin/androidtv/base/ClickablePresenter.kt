package org.jellyfin.androidtv.base

import androidx.leanback.widget.Presenter

abstract class ClickablePresenter : Presenter() {
	abstract fun onItemClicked(item: Any?)
}
