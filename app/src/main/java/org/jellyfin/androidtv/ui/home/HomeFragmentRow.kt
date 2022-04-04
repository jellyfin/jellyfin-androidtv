package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter

interface HomeFragmentRow {
	fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: ArrayObjectAdapter)
}
