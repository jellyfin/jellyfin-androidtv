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
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
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
	private var libraries: List<BaseItemDto>? = null
	private val peopleSamples = mutableMapOf<List<UUID>, List<BaseItemDto>>()
	private val usedPersonIds = mutableSetOf<UUID>()
	private val usedGenres = mutableSetOf<String>()
	private val usedCollectionIds = mutableSetOf<UUID>()
	private val usedStudioIds = mutableSetOf<UUID>()
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

	fun loadRandomItems(): HomeFragmentRow {
		val query = GetItemsRequest(
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			isPlayed = false,
			sortBy = setOf(ItemSortBy.RANDOM),
			// With a start index, ItemRowAdapter uses this limit instead of the chunk size
			startIndex = 0,
			limit = ITEM_LIMIT_RANDOM,
			fields = ItemRepository.browseFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
		)

		return HomeFragmentBrowseRowDefRow(BrowseRowDef(context.getString(R.string.home_section_random_items), query, 0))
	}

	suspend fun loadHiddenGems(userViews: Collection<BaseItemDto>): List<HomeFragmentRow> = userViews
		.filter { it.collectionType == null || it.collectionType in HIDDEN_GEMS_COLLECTION_TYPES }
		.mapNotNull { view ->
			val title = context.getString(R.string.home_row_hidden_gems_in, view.name)
			// isPlayed=false still includes started titles, so drop anything with progress here
			val ids = runCatching {
				api.itemsApi.getItems(
					parentId = view.id,
					includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
					recursive = true,
					isPlayed = false,
					minCommunityRating = HIDDEN_GEMS_MIN_RATING,
					sortBy = setOf(ItemSortBy.RANDOM),
					limit = HIDDEN_GEMS_SAMPLE_SIZE,
					enableTotalRecordCount = false,
				).content.items
			}.onFailure { Timber.w(it, "Unable to load hidden gems") }
				.getOrDefault(emptyList())
				.filter { (it.userData?.playedPercentage ?: 0.0) == 0.0 && (it.userData?.playbackPositionTicks ?: 0) == 0L }
				.map { it.id }
				.take(ITEM_LIMIT_RANDOM)
			loadItemsRow(title, ids)
		}

	suspend fun loadRandomGenre(): HomeFragmentRow? = runCatching {
		val libraryIds = getMainLibraryIds()
		val genre = libraryIds
			.flatMap { libraryId ->
				api.genresApi.getGenres(
					parentId = libraryId,
					includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
				).content.items
			}
			.mapNotNull { it.name }
			.distinct()
			.filter { it !in usedGenres }
			.shuffled()
			.take(GENRE_CHECK_ATTEMPTS)
			.firstOrNull { name ->
				libraryIds.sumOf { libraryId ->
					api.itemsApi.getItems(
						parentId = libraryId,
						genres = setOf(name),
						includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
						recursive = true,
						limit = 0,
						enableTotalRecordCount = true,
					).content.totalRecordCount
				} >= MIN_TITLES_PER_GENRE
			} ?: return null
		usedGenres.add(genre)

		val ids = libraryIds.flatMap { libraryId ->
			api.itemsApi.getItems(
				parentId = libraryId,
				genres = setOf(genre),
				includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
				recursive = true,
				sortBy = setOf(ItemSortBy.RANDOM),
				limit = ITEM_LIMIT_RANDOM,
				enableTotalRecordCount = false,
			).content.items.map { it.id }
		}.shuffled().take(ITEM_LIMIT_RANDOM)
		loadItemsRow(genre, ids)
	}.onFailure { Timber.w(it, "Unable to load genre home row") }.getOrNull()

	suspend fun loadRandomCollection(): HomeFragmentRow? = runCatching {
		val collection = api.itemsApi.getItems(
			includeItemTypes = setOf(BaseItemKind.BOX_SET),
			recursive = true,
			fields = setOf(ItemFields.CHILD_COUNT),
			enableTotalRecordCount = false,
		).content.items
			.filter { (it.childCount ?: 0) >= MIN_TITLES_PER_COLLECTION && it.id !in usedCollectionIds }
			.randomOrNull() ?: return null
		usedCollectionIds.add(collection.id)

		val query = GetItemsRequest(
			parentId = collection.id,
			sortBy = setOf(ItemSortBy.PREMIERE_DATE, ItemSortBy.SORT_NAME),
			startIndex = 0,
			limit = ITEM_LIMIT_RANDOM,
			fields = ItemRepository.browseFields,
			imageTypeLimit = 1,
			enableTotalRecordCount = false,
		)
		HomeFragmentBrowseRowDefRow(BrowseRowDef(collection.name.orEmpty(), query, 0))
	}.onFailure { Timber.w(it, "Unable to load collection home row") }.getOrNull()

	// Series have no last-played date of their own, so it comes from their most recent episode
	suspend fun loadRewatch(): HomeFragmentRow? = runCatching {
		val cutoff = LocalDateTime.now().minusDays(REWATCH_MIN_AGE_DAYS)
		val played = api.itemsApi.getItems(
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
			recursive = true,
			filters = setOf(ItemFilter.IS_PLAYED),
			enableTotalRecordCount = false,
		).content.items

		val lastPlayedByTitle = played
			.mapNotNull { item ->
				val date = item.userData?.lastPlayedDate ?: return@mapNotNull null
				val id = if (item.type == BaseItemKind.EPISODE) item.seriesId else item.id
				id?.let { it to date }
			}
			.groupBy({ it.first }, { it.second })
			.mapValues { (_, dates) -> dates.max() }

		val ids = lastPlayedByTitle.filterValues { it < cutoff }.keys.shuffled().take(ITEM_LIMIT_RANDOM)
		loadItemsRow(context.getString(R.string.home_row_rewatch), ids)
	}.onFailure { Timber.w(it, "Unable to load rewatch home row") }.getOrNull()

	// Most studios have a single title, so pick from studios that recur in a random sample
	suspend fun loadMoreFromStudio(): HomeFragmentRow? = runCatching {
		val libraryIds = getMainLibraryIds()
		val studio = libraryIds
			.flatMap { libraryId ->
				api.itemsApi.getItems(
					parentId = libraryId,
					includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
					recursive = true,
					sortBy = setOf(ItemSortBy.RANDOM),
					limit = STUDIO_SAMPLE_SIZE / libraryIds.size,
					fields = setOf(ItemFields.STUDIOS),
					enableTotalRecordCount = false,
				).content.items
			}
			.flatMap { it.studios.orEmpty() }
			.filter { it.id !in usedStudioIds }
			.groupBy { it.id }
			.values
			.shuffled()
			.sortedByDescending { it.size }
			.map { it.first() }
			.take(STUDIO_CHECK_ATTEMPTS)
			.firstOrNull { studio ->
				libraryIds.sumOf { libraryId ->
					api.itemsApi.getItems(
						parentId = libraryId,
						studioIds = setOf(studio.id),
						includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
						recursive = true,
						limit = 0,
						enableTotalRecordCount = true,
					).content.totalRecordCount
				} >= MIN_TITLES_PER_STUDIO
			} ?: return null
		usedStudioIds.add(studio.id)

		val ids = libraryIds.flatMap { libraryId ->
			api.itemsApi.getItems(
				parentId = libraryId,
				studioIds = setOf(studio.id),
				includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
				recursive = true,
				sortBy = setOf(ItemSortBy.RANDOM),
				limit = ITEM_LIMIT_RANDOM,
				enableTotalRecordCount = false,
			).content.items.map { it.id }
		}.shuffled().take(ITEM_LIMIT_RANDOM)
		loadItemsRow(context.getString(R.string.home_row_more_from, studio.name), ids)
	}.onFailure { Timber.w(it, "Unable to load studio home row") }.getOrNull()

	// The server ignores minCriticRating, so take the top-rated per library and filter here
	suspend fun loadCriticallyAcclaimed(): HomeFragmentRow? = runCatching {
		val ids = getMainLibraryIds().flatMap { libraryId ->
			api.itemsApi.getItems(
				parentId = libraryId,
				includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
				recursive = true,
				isPlayed = false,
				sortBy = setOf(ItemSortBy.CRITIC_RATING),
				sortOrder = setOf(SortOrder.DESCENDING),
				limit = CRITICS_SAMPLE_SIZE,
				enableTotalRecordCount = false,
			).content.items
		}
			.filter { (it.criticRating ?: 0f) >= CRITICS_MIN_RATING && (it.userData?.playbackPositionTicks ?: 0) == 0L }
			.map { it.id }
			.shuffled()
			.take(ITEM_LIMIT_RANDOM)
		loadItemsRow(context.getString(R.string.home_section_critically_acclaimed), ids)
	}.onFailure { Timber.w(it, "Unable to load critically acclaimed home row") }.getOrNull()

	private fun loadItemsRow(title: String, ids: List<UUID>): HomeFragmentRow? {
		// An empty id list would match everything
		if (ids.isEmpty()) return null

		val query = GetItemsRequest(
			ids = ids,
			sortBy = setOf(ItemSortBy.RANDOM),
			startIndex = 0,
			limit = ids.size,
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
		loadPersonRow(R.string.home_row_starring, getMainLibraryIds(), MIN_TITLES_PER_PERSON) { people ->
			people.filter { it.type == PersonKind.ACTOR }.take(TOP_BILLED_ACTORS)
		}

	suspend fun loadDirectedBy(): HomeFragmentRow? =
		loadPersonRow(R.string.home_row_directed_by, getMainLibraryIds(), MIN_TITLES_PER_PERSON) { people ->
			people.filter { it.type == PersonKind.DIRECTOR }
		}

	suspend fun loadStandupComedian(): HomeFragmentRow? =
		loadPersonRow(R.string.home_row_standup_comedian, getStandupLibraryIds(), MIN_SPECIALS_PER_COMEDIAN) { people ->
			people.filter { it.type == PersonKind.ACTOR }.take(TOP_BILLED_COMEDIANS)
		}

	// The server can't filter people by title count, so sample the libraries and verify candidates
	private suspend fun loadPersonRow(
		titleRes: Int,
		libraryIds: List<UUID>,
		minTitles: Int,
		candidates: (List<BaseItemPerson>) -> List<BaseItemPerson>,
	): HomeFragmentRow? = runCatching {
		if (libraryIds.isEmpty()) return null

		val person = getPeopleSample(libraryIds)
			.flatMap { candidates(it.people.orEmpty()) }
			.filter { it.id !in usedPersonIds }
			.groupBy { it.id }
			.values
			.shuffled()
			// Frequent people in the sample are the likeliest to pass; shuffle breaks ties
			.sortedByDescending { it.size }
			.map { it.first() }
			.take(PERSON_CHECK_ATTEMPTS)
			.firstOrNull { countPersonTitles(it.id, libraryIds) >= minTitles } ?: return null
		usedPersonIds.add(person.id)

		val ids = getPersonTitleIds(person.id, libraryIds).shuffled().take(ITEM_LIMIT_HISTORY_ROW)
		loadItemsRow(context.getString(titleRes, person.name), ids)
	}.onFailure { Timber.w(it, "Unable to load person home row") }.getOrNull()

	private suspend fun getPeopleSample(libraryIds: List<UUID>): List<BaseItemDto> = peopleSamples.getOrPut(libraryIds) {
		libraryIds.flatMap { libraryId ->
			api.itemsApi.getItems(
				parentId = libraryId,
				includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
				recursive = true,
				sortBy = setOf(ItemSortBy.RANDOM),
				limit = PERSON_SAMPLE_SIZE / libraryIds.size,
				fields = setOf(ItemFields.PEOPLE),
				enableTotalRecordCount = false,
			).content.items
		}
	}

	private suspend fun countPersonTitles(personId: UUID, libraryIds: List<UUID>): Int = libraryIds.sumOf { libraryId ->
		api.itemsApi.getItems(
			parentId = libraryId,
			personIds = setOf(personId),
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			limit = 0,
			enableTotalRecordCount = true,
		).content.totalRecordCount
	}

	private suspend fun getPersonTitleIds(personId: UUID, libraryIds: List<UUID>): List<UUID> = libraryIds.flatMap { libraryId ->
		api.itemsApi.getItems(
			parentId = libraryId,
			personIds = setOf(personId),
			includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
			recursive = true,
			limit = ITEM_LIMIT_HISTORY_ROW,
			enableTotalRecordCount = false,
		).content.items.map { it.id }
	}

	private suspend fun getLibraries(): List<BaseItemDto> =
		libraries ?: api.userViewsApi.getUserViews().content.items.also { libraries = it }

	private fun BaseItemDto.isStandupLibrary() = name.orEmpty().contains(STANDUP_LIBRARY_PATTERN)

	private suspend fun getMainLibraryIds(): List<UUID> = getLibraries()
		.filter { it.collectionType in MAIN_LIBRARY_TYPES && !it.isStandupLibrary() }
		.map { it.id }

	private suspend fun getStandupLibraryIds(): List<UUID> = getLibraries()
		.filter { it.isStandupLibrary() }
		.map { it.id }

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
		private const val HIDDEN_GEMS_SAMPLE_SIZE = 100
		private const val GENRE_CHECK_ATTEMPTS = 10
		private const val MIN_TITLES_PER_GENRE = 10
		private const val MIN_TITLES_PER_COLLECTION = 2
		private const val REWATCH_MIN_AGE_DAYS = 365L
		private const val STUDIO_SAMPLE_SIZE = 100
		private const val STUDIO_CHECK_ATTEMPTS = 10
		private const val MIN_TITLES_PER_STUDIO = 3
		private const val CRITICS_SAMPLE_SIZE = 150
		private const val CRITICS_MIN_RATING = 85f
		private const val TOP_BILLED_COMEDIANS = 2
		private const val MIN_SPECIALS_PER_COMEDIAN = 2
		private val STANDUP_LIBRARY_PATTERN = Regex("stand.?up", RegexOption.IGNORE_CASE)
		private val MAIN_LIBRARY_TYPES = setOf(CollectionType.MOVIES, CollectionType.TVSHOWS)
	}
}
