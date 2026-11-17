package org.jellyfin.androidtv.ui.home

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import java.time.LocalDateTime

class HomeFragmentHelper(
	private val context: Context,
	private val userRepository: UserRepository,
	private val userPreferences: UserPreferences,
) {
	fun loadRecentlyAdded(userViews: Collection<BaseItemDto>): HomeFragmentRow {
		return HomeFragmentLatestRow(userRepository, userViews)
	}

	fun loadResume(title: String, includeMediaTypes: Collection<MediaType>): HomeFragmentRow {
		val query = GetResumeItemsRequest(
			limit = ITEM_LIMIT_RESUME,
			fields = ItemRepository.browseFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
			mediaTypes = includeMediaTypes,
			excludeItemTypes = setOf(BaseItemKind.AUDIO_BOOK),
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, 0, false, true, arrayOf(ChangeTriggerType.TvPlayback, ChangeTriggerType.MoviePlayback)))
	}

	fun loadResumeVideo(): HomeFragmentRow {
		return loadResume(context.getString(R.string.lbl_continue_watching), listOf(MediaType.VIDEO))
	}

	fun loadResumeAudio(): HomeFragmentRow {
		return loadResume(context.getString(R.string.continue_listening), listOf(MediaType.AUDIO))
	}

	fun loadLatestLiveTvRecordings(): HomeFragmentRow {
		val query = GetRecordingsRequest(
			fields = ItemRepository.itemFields,
			enableImages = true,
			limit = ITEM_LIMIT_RECORDINGS
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_recordings), query))
	}

	fun loadNextUp(): HomeFragmentRow {
		val maxDays = userPreferences[UserPreferences.homeNextUpMaxDays]
		val nextUpDateCutoff = maxDays.takeIf { it > 0 }?.let { LocalDateTime.now().minusDays(it.toLong()) }

		val query = GetNextUpRequest(
			imageTypeLimit = 1,
			limit = ITEM_LIMIT_NEXT_UP,
			enableResumable = false,
			fields = ItemRepository.browseFields,
			nextUpDateCutoff = nextUpDateCutoff,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_next_up), query, arrayOf(ChangeTriggerType.TvPlayback)))
	}

	fun loadOnNow(): HomeFragmentRow {
		val query = GetRecommendedProgramsRequest(
			isAiring = true,
			fields = ItemRepository.itemFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
			limit = ITEM_LIMIT_ON_NOW
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.lbl_on_now), query))
	}

	fun loadRandomItems(): HomeFragmentRow =
		loadUnplayedRandom(R.string.home_section_random_items, minCommunityRating = null)

	fun loadHiddenGems(): HomeFragmentRow =
		loadUnplayedRandom(R.string.home_section_hidden_gems, minCommunityRating = HIDDEN_GEMS_MIN_RATING)

	private fun loadUnplayedRandom(titleRes: Int, minCommunityRating: Double?): HomeFragmentRow {
		val query = GetItemsRequest(
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			isPlayed = false,
			minCommunityRating = minCommunityRating,
			sortBy = setOf(ItemSortBy.RANDOM),
			// With a start index, ItemRowAdapter uses this limit instead of the chunk size
			startIndex = 0,
			limit = ITEM_LIMIT_RANDOM,
			fields = ItemRepository.browseFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(titleRes), query, 0))
	}

	fun loadRecentlyAddedAll(): HomeFragmentRow {
		val query = GetLatestMediaRequest(
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
			groupItems = true,
			limit = ITEM_LIMIT_RECENTLY_ADDED_ALL,
			fields = ItemRepository.browseFields,
			imageTypeLimit = 1,
		)

		return HomeFragmentBrowseRowDefRow(
			BrowseRowDef(context.getString(R.string.home_section_recently_added_all), query, arrayOf(ChangeTriggerType.LibraryUpdated))
		)
	}

	fun loadLatestReleases(): HomeFragmentRow {
		val query = GetItemsRequest(
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			maxPremiereDate = LocalDateTime.now(),
			sortBy = setOf(ItemSortBy.PREMIERE_DATE),
			sortOrder = setOf(SortOrder.DESCENDING),
			startIndex = 0,
			limit = ITEM_LIMIT_LATEST_RELEASES,
			fields = ItemRepository.browseFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.home_section_latest_releases), query, 0))
	}

	companion object {
		// Maximum amount of items loaded for a row
		private const val ITEM_LIMIT_RESUME = 50
		private const val ITEM_LIMIT_RECORDINGS = 40
		private const val ITEM_LIMIT_NEXT_UP = 50
		private const val ITEM_LIMIT_ON_NOW = 20
		private const val ITEM_LIMIT_RANDOM = 25
		private const val ITEM_LIMIT_RECENTLY_ADDED_ALL = 50
		private const val ITEM_LIMIT_LATEST_RELEASES = 25
		private const val HIDDEN_GEMS_MIN_RATING = 7.5
	}
}
