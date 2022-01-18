package org.jellyfin.androidtv.ui.home

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.data.querying.StdItemQuery
import org.jellyfin.androidtv.data.querying.ViewQuery
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.apiclient.model.entities.LocationType
import org.jellyfin.apiclient.model.entities.MediaType
import org.jellyfin.apiclient.model.livetv.RecommendedProgramQuery
import org.jellyfin.apiclient.model.livetv.RecordingQuery
import org.jellyfin.apiclient.model.querying.*

class HomeFragmentHelper(
	private val context: Context
) {
	fun loadRecentlyAdded(views: ItemsResult): HomeFragmentRow {
		return HomeFragmentLatestRow(context, views)
	}

	fun loadLibraryTiles(): HomeFragmentRow {
		val query = ViewQuery()
		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_my_media), query))
	}

	fun loadResume(title: String, includeMediaTypes: Array<String>): HomeFragmentRow {
		val query = StdItemQuery().apply {
			mediaTypes = includeMediaTypes
			recursive = true
			imageTypeLimit = 1
			enableTotalRecordCount = false
			collapseBoxSetItems = false
			excludeLocationTypes = arrayOf(LocationType.Virtual)
			limit = ITEM_LIMIT_RESUME
			filters = arrayOf(ItemFilter.IsResumable)
			sortBy = arrayOf(ItemSortBy.DatePlayed)
			sortOrder = org.jellyfin.apiclient.model.entities.SortOrder.Descending
		}

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, 0, false, true, arrayOf(ChangeTriggerType.VideoQueueChange, ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback)))
	}

	fun loadResumeVideo(): HomeFragmentRow {
		return loadResume(context.getString(R.string.lbl_continue_watching), arrayOf(MediaType.Video))
	}

	fun loadResumeAudio(): HomeFragmentRow {
		return loadResume(context.getString(R.string.lbl_continue_watching), arrayOf(MediaType.Audio))
	}

	fun loadLatestLiveTvRecordings(): HomeFragmentRow {
		val query = RecordingQuery().apply {
			fields = arrayOf(
				ItemFields.Overview,
				ItemFields.PrimaryImageAspectRatio,
				ItemFields.ChildCount
			)

			userId = TvApp.getApplication()!!.currentUser!!.id
			enableImages = true
			limit = ITEM_LIMIT_RECORDINGS
		}

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_recordings), query))
	}

	fun loadNextUp(): HomeFragmentRow {
		val query = NextUpQuery().apply {
			userId = TvApp.getApplication()!!.currentUser!!.id
			imageTypeLimit = 1
			limit = ITEM_LIMIT_NEXT_UP
			fields = arrayOf(
				ItemFields.PrimaryImageAspectRatio,
				ItemFields.Overview,
				ItemFields.ChildCount
			)
		}

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_next_up), query, arrayOf(ChangeTriggerType.TvPlayback)))
	}

	fun loadOnNow(): HomeFragmentRow {
		val query = RecommendedProgramQuery().apply {
			isAiring = true
			fields = arrayOf(
				ItemFields.Overview,
				ItemFields.PrimaryImageAspectRatio,
				ItemFields.ChannelInfo,
				ItemFields.ChildCount
			)
			userId = TvApp.getApplication()!!.currentUser!!.id
			imageTypeLimit = 1
			enableTotalRecordCount = false
			limit = ITEM_LIMIT_ON_NOW
		}

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_on_now), query))
	}

	companion object {
		// Maximum amount of items loaded for a row
		private const val ITEM_LIMIT_RESUME = 50
		private const val ITEM_LIMIT_RECORDINGS = 40
		private const val ITEM_LIMIT_NEXT_UP = 50
		private const val ITEM_LIMIT_ON_NOW = 20
	}
}
