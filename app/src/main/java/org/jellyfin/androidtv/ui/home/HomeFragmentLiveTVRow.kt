package org.jellyfin.androidtv.ui.home

import android.app.Activity
import android.content.Context
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.LiveTvOption
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

class HomeFragmentLiveTVRow(
	private val activity: Activity,
	private val userRepository: UserRepository,
	private val navigationRepository: NavigationRepository,
) : HomeFragmentRow, OnItemViewClickedListener {
	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		val header = HeaderItem(rowsAdapter.size().toLong(), activity.getString(R.string.pref_live_tv_cat))
		val adapter = ArrayObjectAdapter(GridButtonPresenter())

		// Live TV Guide button
		adapter.add(GridButton(LiveTvOption.LIVE_TV_GUIDE_OPTION_ID, activity.getString(R.string.lbl_live_tv_guide), R.drawable.tile_port_guide))
		// Live TV Recordings button
		adapter.add(GridButton(LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID, activity.getString(R.string.lbl_recorded_tv), R.drawable.tile_port_record))
		if (Utils.canManageRecordings(userRepository.currentUser.value)) {
			// Recording Schedule button
			adapter.add(GridButton(LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID, activity.getString(R.string.lbl_schedule), R.drawable.tile_port_time))
			// Recording Series button
			adapter.add(GridButton(LiveTvOption.LIVE_TV_SERIES_OPTION_ID, activity.getString(R.string.lbl_series), R.drawable.tile_port_series_timer))
		}

		rowsAdapter.add(ListRow(header, adapter))
	}

	override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
		if (item !is GridButton) return

		when (item.id) {
			LiveTvOption.LIVE_TV_GUIDE_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvGuide)
			LiveTvOption.LIVE_TV_SCHEDULE_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvSchedule)
			LiveTvOption.LIVE_TV_RECORDINGS_OPTION_ID -> navigationRepository.navigate(Destinations.liveTvRecordings)
			LiveTvOption.LIVE_TV_SERIES_OPTION_ID -> {
				val folder = BaseItemDto(
					id = UUID.randomUUID(),
					type = BaseItemKind.FOLDER,
					collectionType = "SeriesTimers",
					name = activity.getString(R.string.lbl_series_recordings),
				)
				navigationRepository.navigate(Destinations.libraryBrowser(folder))
			}
		}
	}
}
