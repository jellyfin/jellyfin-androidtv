package org.jellyfin.androidtv.ui.home

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.querying.ViewQuery
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter

import org.koin.java.KoinJavaComponent.get

class HomeFragmentBrowseRowDefRow(
	private val browseRowDef: BrowseRowDef
) : HomeFragmentRow {
	override fun addToRowsAdapter(cardPresenter: CardPresenter, rowsAdapter: ArrayObjectAdapter) {
		val header = HeaderItem(browseRowDef.headerText)
		val preferParentThumb = get<UserPreferences>(UserPreferences::class.java)[UserPreferences.seriesThumbnailsEnabled]

		// Some of these members are probably never used and could be removed
		val rowAdapter = when (browseRowDef.queryType) {
			QueryType.NextUp -> ItemRowAdapter(browseRowDef.nextUpQuery, preferParentThumb, cardPresenter, rowsAdapter)
			QueryType.LatestItems -> ItemRowAdapter(browseRowDef.latestItemsQuery, preferParentThumb, cardPresenter, rowsAdapter)
			QueryType.Season -> ItemRowAdapter(browseRowDef.seasonQuery, cardPresenter, rowsAdapter)
			QueryType.Upcoming -> ItemRowAdapter(browseRowDef.upcomingQuery, cardPresenter, rowsAdapter)
			QueryType.Views -> ItemRowAdapter(ViewQuery(), cardPresenter, rowsAdapter)
			QueryType.SimilarSeries -> ItemRowAdapter(browseRowDef.similarQuery, QueryType.SimilarSeries, cardPresenter, rowsAdapter)
			QueryType.SimilarMovies -> ItemRowAdapter(browseRowDef.similarQuery, QueryType.SimilarMovies, cardPresenter, rowsAdapter)
			QueryType.Persons -> ItemRowAdapter(browseRowDef.personsQuery, browseRowDef.chunkSize, cardPresenter, rowsAdapter)
			QueryType.LiveTvChannel -> ItemRowAdapter(browseRowDef.tvChannelQuery, 40, cardPresenter, rowsAdapter)
			QueryType.LiveTvProgram -> ItemRowAdapter(browseRowDef.programQuery, cardPresenter, rowsAdapter)
			QueryType.LiveTvRecording -> ItemRowAdapter(browseRowDef.recordingQuery, browseRowDef.chunkSize, cardPresenter, rowsAdapter)
			QueryType.LiveTvRecordingGroup -> ItemRowAdapter(browseRowDef.recordingGroupQuery, cardPresenter, rowsAdapter)
			else -> ItemRowAdapter(browseRowDef.query, browseRowDef.chunkSize, browseRowDef.preferParentThumb, browseRowDef.isStaticHeight, cardPresenter, rowsAdapter, browseRowDef.queryType)
		}

		rowAdapter.setReRetrieveTriggers(browseRowDef.changeTriggers)
		val row = ListRow(header, rowAdapter)
		rowAdapter.setRow(row)
		rowAdapter.Retrieve()
		rowsAdapter.add(row)
	}
}
