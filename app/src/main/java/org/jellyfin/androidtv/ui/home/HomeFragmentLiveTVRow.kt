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
import org.jellyfin.androidtv.model.repository.SerializerRepository
import org.jellyfin.androidtv.presentation.CardPresenter
import org.jellyfin.androidtv.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.apiclient.model.dto.BaseItemDto

class HomeFragmentLiveTVRow(val activity: Activity) : HomeFragmentRow(), OnItemViewClickedListener {
	override fun addToRowsAdapter(cardPresenter: CardPresenter?, rowsAdapter: ArrayObjectAdapter) {
		val header = HeaderItem(rowsAdapter.size().toLong(), activity.getString(R.string.pref_live_tv_cat))
		val adapter = ArrayObjectAdapter(GridButtonPresenter())

		// Live TV Guide button
		adapter.add(GridButton(TvApp.LIVE_TV_GUIDE_OPTION_ID, activity.getString(R.string.lbl_live_tv_guide), R.drawable.tile_port_guide))
		// Live TV Recordings button
		adapter.add(GridButton(TvApp.LIVE_TV_RECORDINGS_OPTION_ID, activity.getString(R.string.lbl_recorded_tv), R.drawable.tile_port_record))
		if (TvApp.getApplication().canManageRecordings()) {
			// Recording Schedule button
			adapter.add(GridButton(TvApp.LIVE_TV_SCHEDULE_OPTION_ID, activity.getString(R.string.lbl_schedule), R.drawable.tile_port_time))
			// Recording Series button
			adapter.add(GridButton(TvApp.LIVE_TV_SERIES_OPTION_ID, activity.getString(R.string.lbl_series), R.drawable.tile_port_series_timer))
		}

		rowsAdapter.add(ListRow(header, adapter))
	}

	override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
		if (item !is GridButton) return

		when (item.id) {
			TvApp.LIVE_TV_GUIDE_OPTION_ID -> {
				activity.startActivity(
					Intent(activity, LiveTvGuideActivity::class.java)
				)
			}
			TvApp.LIVE_TV_RECORDINGS_OPTION_ID -> {
				activity.startActivity(
					Intent(activity, BrowseRecordingsActivity::class.java).apply {
						putExtra(
							"Folder",
							SerializerRepository.serializer.SerializeToString(
								BaseItemDto().apply {
									id = ""
									name = activity.getString(R.string.lbl_recorded_tv)
								}
							))
					}
				)
			}
			TvApp.LIVE_TV_SCHEDULE_OPTION_ID -> {
				activity.startActivity(
					Intent(activity, BrowseScheduleActivity::class.java)
				)
			}
			TvApp.LIVE_TV_SERIES_OPTION_ID -> {
				activity.startActivity(
					Intent(activity, UserViewActivity::class.java).apply {
						putExtra(
							"Folder",
							SerializerRepository.serializer.SerializeToString(
								BaseItemDto().apply {
									id = "SERIESTIMERS"
									collectionType = "SeriesTimers"
									name = activity.getString(R.string.lbl_series_recordings)
								}
							)
						)
					}
				)
			}
		}
	}
}
