package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter

class HomeFragmentNowPlayingRow(
	private val mediaManager: MediaManager
) : HomeFragmentRow {
	private var row: ListRow? = null

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		update(context, rowsAdapter)
	}

	private fun add(context: Context, rowsAdapter: MutableObjectAdapter<Row>) {
		if (row != null) return

		row = ListRow(HeaderItem(context.getString(R.string.lbl_now_playing)), mediaManager.managedAudioQueue)
		rowsAdapter.add(0, row!!)
	}

	private fun remove(rowsAdapter: MutableObjectAdapter<Row>) {
		if (row == null) return

		rowsAdapter.remove(row!!)
		row = null
	}

	fun update(context: Context, rowsAdapter: MutableObjectAdapter<Row>) {
		if (mediaManager.hasAudioQueueItems()) {
			if (row != null) {
				row = ListRow(HeaderItem(context.getString(R.string.lbl_now_playing)), mediaManager.managedAudioQueue)
				rowsAdapter.set(0, row!!)
			}
			else add(context, rowsAdapter)
		}
		else remove(rowsAdapter)
	}
}
