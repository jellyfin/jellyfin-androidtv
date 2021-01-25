package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.presentation.CardPresenter

class HomeFragmentNowPlayingRow(
	private val context: Context
) : HomeFragmentRow {
	private var row: ListRow? = null

	override fun addToRowsAdapter(cardPresenter: CardPresenter, rowsAdapter: ArrayObjectAdapter) {
		update(rowsAdapter)
	}

	private fun add(rowsAdapter: ArrayObjectAdapter) {
		if (row != null) return

		row = ListRow(HeaderItem(context.getString(R.string.lbl_now_playing)), MediaManager.getManagedAudioQueue())
		rowsAdapter.add(0, row)
	}

	private fun remove(rowsAdapter: ArrayObjectAdapter) {
		if (row == null) return

		rowsAdapter.remove(row)
		row = null
	}

	fun update(rowsAdapter: ArrayObjectAdapter) {
		if (MediaManager.isPlayingAudio()) add(rowsAdapter)
		else remove(rowsAdapter)
	}
}
