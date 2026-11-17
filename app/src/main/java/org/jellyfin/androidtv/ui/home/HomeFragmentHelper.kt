package org.jellyfin.androidtv.ui.home

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ChangeTriggerType
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PersonKind
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetLatestMediaRequest
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetRecommendedProgramsRequest
import org.jellyfin.sdk.model.api.request.GetRecordingsRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class HomeFragmentHelper(
	private val context: Context,
	private val api: ApiClient,
	private val userRepository: UserRepository,
	private val userPreferences: UserPreferences,
) {
	private data class WatchedTitle(val id: UUID, val name: String)

	private var recentlyWatched: List<WatchedTitle>? = null
	private var peopleSample: List<BaseItemDto>? = null
	private val usedPersonIds = mutableSetOf<UUID>()
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
		loadUnplayedRandom(context.getString(R.string.home_section_random_items), minCommunityRating = null, parentId = null)

	fun loadHiddenGems(userViews: Collection<BaseItemDto>): List<HomeFragmentRow> = userViews
		.filter { it.collectionType == null || it.collectionType in HIDDEN_GEMS_COLLECTION_TYPES }
		.map { view ->
			val title = context.getString(R.string.home_row_hidden_gems_in, view.name)
			loadUnplayedRandom(title, minCommunityRating = HIDDEN_GEMS_MIN_RATING, parentId = view.id)
		}

	private fun loadUnplayedRandom(title: String, minCommunityRating: Double?, parentId: UUID?): HomeFragmentRow {
		val query = GetItemsRequest(
			parentId = parentId,
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

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(title, query, 0))
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

	suspend fun loadBecauseYouWatched(): HomeFragmentRow? {
		val seed = getRecentlyWatched().take(BECAUSE_YOU_WATCHED_SEED_POOL).randomOrNull() ?: return null
		val query = GetSimilarItemsRequest(
			itemId = seed.id,
			fields = ItemRepository.browseFields,
			limit = ITEM_LIMIT_HISTORY_ROW,
		)

		return HomeFragmentBrowseRowDefRow(
			BrowseRowDef(context.getString(R.string.because_you_watched, seed.name), query, QueryType.SimilarMovies)
		)
	}

	suspend fun loadStarring(): HomeFragmentRow? =
		loadPersonRow(R.string.home_row_starring) { people ->
			people.filter { it.type == PersonKind.ACTOR }.take(TOP_BILLED_ACTORS)
		}

	suspend fun loadDirectedBy(): HomeFragmentRow? =
		loadPersonRow(R.string.home_row_directed_by) { people ->
			people.filter { it.type == PersonKind.DIRECTOR }
		}

	// The server can't filter people by title count, so sample the library and verify candidates
	private suspend fun loadPersonRow(
		titleRes: Int,
		candidates: (List<BaseItemPerson>) -> List<BaseItemPerson>,
	): HomeFragmentRow? = runCatching {
		val person = getPeopleSample()
			.flatMap { candidates(it.people.orEmpty()) }
			.filter { it.id !in usedPersonIds }
			.groupBy { it.id }
			.values
			.shuffled()
			// Frequent people in the sample are the likeliest to pass; shuffle breaks ties
			.sortedByDescending { it.size }
			.map { it.first() }
			.take(PERSON_CHECK_ATTEMPTS)
			.firstOrNull { hasEnoughTitles(it.id) } ?: return null
		usedPersonIds.add(person.id)

		val query = GetItemsRequest(
			personIds = setOf(person.id),
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			sortBy = setOf(ItemSortBy.RANDOM),
			startIndex = 0,
			limit = ITEM_LIMIT_HISTORY_ROW,
			fields = ItemRepository.browseFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
		)

		HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(titleRes, person.name), query, 0))
	}.onFailure { Timber.w(it, "Unable to load person home row") }.getOrNull()

	private suspend fun getPeopleSample(): List<BaseItemDto> = peopleSample ?: api.itemsApi.getItems(
		includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
		recursive = true,
		sortBy = setOf(ItemSortBy.RANDOM),
		limit = PERSON_SAMPLE_SIZE,
		fields = setOf(ItemFields.PEOPLE),
		enableTotalRecordCount = false,
	).content.items.also { peopleSample = it }

	private suspend fun hasEnoughTitles(personId: UUID): Boolean {
		val count = api.itemsApi.getItems(
			personIds = setOf(personId),
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			limit = 0,
			enableTotalRecordCount = true,
		).content.totalRecordCount

		return count >= MIN_TITLES_PER_PERSON
	}

	// Recently played movies and series (episodes collapsed to their series), newest first
	private suspend fun getRecentlyWatched(): List<WatchedTitle> {
		recentlyWatched?.let { return it }

		val titles = runCatching {
			api.itemsApi.getItems(
				includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
				recursive = true,
				filters = setOf(ItemFilter.IS_PLAYED),
				sortBy = setOf(ItemSortBy.DATE_PLAYED),
				sortOrder = setOf(SortOrder.DESCENDING),
				limit = HISTORY_LIMIT,
				enableTotalRecordCount = false,
			).content.items
		}.onFailure { Timber.w(it, "Unable to load watch history") }
			.getOrDefault(emptyList())
			.mapNotNull { item ->
				if (item.type == BaseItemKind.EPISODE) item.seriesId?.let { WatchedTitle(it, item.seriesName.orEmpty()) }
				else WatchedTitle(item.id, item.name.orEmpty())
			}
			.distinctBy { it.id }
			.take(MAX_RECENT_TITLES)

		recentlyWatched = titles
		return titles
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
		private val HIDDEN_GEMS_COLLECTION_TYPES = setOf(CollectionType.MOVIES, CollectionType.TVSHOWS)
		private const val ITEM_LIMIT_HISTORY_ROW = 25
		private const val HISTORY_LIMIT = 50
		private const val MAX_RECENT_TITLES = 20
		private const val BECAUSE_YOU_WATCHED_SEED_POOL = 5
		private const val TOP_BILLED_ACTORS = 5
		private const val PERSON_SAMPLE_SIZE = 100
		private const val PERSON_CHECK_ATTEMPTS = 10
		private const val MIN_TITLES_PER_PERSON = 5
	}
}
