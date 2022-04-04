package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.presentation.CardPresenter

class HomeFragmentNowPlayingRow(
	private val mediaManager: MediaManager
) : HomeFragmentRow {
	private var row: ListRow? = null

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: ArrayObjectAdapter) {
		update(context, rowsAdapter)
	}

	private fun add(context: Context, rowsAdapter: ArrayObjectAdapter) {
		if (row != null) return

		row = ListRow(HeaderItem(context.getString(R.string.lbl_now_playing)), mediaManager.managedAudioQueue)
		rowsAdapter.add(0, row)
	}

	private fun remove(rowsAdapter: ArrayObjectAdapter) {
		if (row == null) return

		rowsAdapter.remove(row)
		row = null
	}

	fun update(context: Context, rowsAdapter: ArrayObjectAdapter) {
		if (mediaManager.hasAudioQueueItems()) {
			if (row != null) {
				row = ListRow(HeaderItem(context.getString(R.string.lbl_now_playing)), mediaManager.managedAudioQueue)
				rowsAdapter.replace(0, row)
			}
			else add(context, rowsAdapter)
		}
		else remove(rowsAdapter)
	}
}
