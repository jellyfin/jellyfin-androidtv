package org.jellyfin.androidtv.ui.home

import androidx.leanback.widget.ArrayObjectAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter

interface HomeFragmentRow {
	fun addToRowsAdapter(cardPresenter: CardPresenter, rowsAdapter: ArrayObjectAdapter)
}
