package org.jellyfin.androidtv.ui.home

import android.app.Activity
import android.content.Intent

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.browsing.BrowseRecordingsActivity
import org.jellyfin.androidtv.browsing.BrowseScheduleActivity
import org.jellyfin.androidtv.browsing.UserViewActivity
import org.jellyfin.androidtv.livetv.LiveTvGuideActivity
import org.jellyfin.androidtv.presentation.CardPresenter
import org.jellyfin.androidtv.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.apiclient.model.dto.BaseItemDto

class HomeFragmentLiveTVRow(val activity: Activity?) : HomeFragmentRow(), OnItemViewClickedListener {
	override fun addToRowsAdapter(cardPresenter: CardPresenter?, rowsAdapter: ArrayObjectAdapter?) {
		val header = HeaderItem(rowsAdapter!!.size().toLong(), activity?.getString(R.string.pref_live_tv_cat))
		val presenter = GridButtonPresenter()
		val adapter = ArrayObjectAdapter(presenter)

		// Live TV Guide button
		adapter.add(GridButton(TvApp.LIVE_TV_GUIDE_OPTION_ID, activity?.getString(R.string.lbl_live_tv_guide), R.drawable.tile_port_guide))
		// Live TV Recordings button
		adapter.add(GridButton(TvApp.LIVE_TV_RECORDINGS_OPTION_ID, activity?.getString(R.string.lbl_recorded_tv), R.drawable.tile_port_record))
		if (TvApp.getApplication().canManageRecordings()) {
			// Recording Schedule button
			adapter.add(GridButton(TvApp.LIVE_TV_SCHEDULE_OPTION_ID, activity?.getString(R.string.lbl_schedule), R.drawable.tile_port_time))
			// Recording Series button
			adapter.add(GridButton(TvApp.LIVE_TV_SERIES_OPTION_ID, activity?.getString(R.string.lbl_series), R.drawable.tile_port_series_timer))
		}

		rowsAdapter.add(ListRow(header, adapter))
	}

	override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
		if (item !is GridButton) return

		when (item.id) {
			TvApp.LIVE_TV_GUIDE_OPTION_ID -> {
				val guide = Intent(activity, LiveTvGuideActivity::class.java)
				activity!!.startActivity(guide)
			}
			TvApp.LIVE_TV_RECORDINGS_OPTION_ID -> {
				val recordings = Intent(activity, BrowseRecordingsActivity::class.java)
				val folder = BaseItemDto()
				folder.id = ""
				folder.name = activity?.getString(R.string.lbl_recorded_tv)
				recordings.putExtra("Folder", TvApp.getApplication().serializer.SerializeToString(folder))
				activity!!.startActivity(recordings)
			}
			TvApp.LIVE_TV_SCHEDULE_OPTION_ID -> {
				val schedule = Intent(activity, BrowseScheduleActivity::class.java)
				activity!!.startActivity(schedule)
			}
			TvApp.LIVE_TV_SERIES_OPTION_ID -> {
				val seriesIntent = Intent(activity, UserViewActivity::class.java)
				val seriesTimers = BaseItemDto()
				seriesTimers.id = "SERIESTIMERS"
				seriesTimers.collectionType = "SeriesTimers"
				seriesTimers.name = activity?.getString(R.string.lbl_series_recordings)
				seriesIntent.putExtra("Folder", TvApp.getApplication().serializer.SerializeToString(seriesTimers))
				activity!!.startActivity(seriesIntent)
			}
		}
	}
}
