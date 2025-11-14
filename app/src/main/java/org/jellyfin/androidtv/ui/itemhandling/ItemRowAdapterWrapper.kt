package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.data.model.FilterOptions
import org.jellyfin.androidtv.data.querying.GetUserViewsRequest
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.ui.browsing.BrowseGridFragment
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.util.apiclient.EmptyResponse
import org.jellyfin.sdk.model.api.ItemSortBy
import timber.log.Timber

class ItemRowAdapterWrapper(
	private val adapter: ItemRowAdapter,
	private val coroutineScope: CoroutineScope
) {
	private val _items = MutableStateFlow<List<BaseRowItem>>(emptyList())
	val items: StateFlow<List<BaseRowItem>> = _items.asStateFlow()

	private val _isLoading = MutableStateFlow(false)
	val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

	private val _totalItems = MutableStateFlow(0)
	val totalItems: StateFlow<Int> = _totalItems.asStateFlow()

	private val _error = MutableStateFlow<String?>(null)
	val error: StateFlow<String?> = _error.asStateFlow()

	private val _filters = MutableStateFlow<FilterOptions?>(null)
	val filters: StateFlow<FilterOptions?> = _filters.asStateFlow()

	private val _startLetter = MutableStateFlow<String?>(null)
	val startLetter: StateFlow<String?> = _startLetter.asStateFlow()

	private val _sortBy = MutableStateFlow<ItemSortBy?>(null)
	val sortBy: StateFlow<ItemSortBy?> = _sortBy.asStateFlow()

	fun setRetrieveListener(lifecycle: Lifecycle) {
		adapter.setRetrieveFinishedListener(object : EmptyResponse(lifecycle) {
			override fun onResponse() {
				_isLoading.value = false
				_totalItems.value = adapter.totalItems
				_error.value = null
				_filters.value = adapter.filters
				_sortBy.value = adapter.sortBy
				updateItems()
				Timber.d("Retrieve finished: itemsLoaded=${adapter.itemsLoaded}, total=${adapter.totalItems}")
			}

			override fun onError(exception: Exception) {
				_isLoading.value = false
				_error.value = exception.message
				Timber.e(exception, "Failed to retrieve items")
			}
		})
	}

	private fun updateItems() {
		_items.value = (0 until adapter.size())
			.mapNotNull { adapter.get(it) as? BaseRowItem }
			.also { Timber.d("Updated items: ${it.size} items in wrapper") }
	}

	fun retrieve() {
		Timber.d("Starting initial retrieve")
		_isLoading.value = true
		_error.value = null
		adapter.Retrieve()
	}

	fun loadMoreItemsIfNeeded(position: Int) {
		if (_isLoading.value) {
			Timber.d("Already loading, skipping loadMoreItemsIfNeeded")
			return
		}

		Timber.d("Calling adapter.loadMoreItemsIfNeeded for position $position")
		_isLoading.value = true
		adapter.loadMoreItemsIfNeeded(position)
	}

	fun setSortBy(sortOption: BrowseGridFragment.SortOption) {
		adapter.setSortBy(sortOption)
		_sortBy.value = adapter.sortBy
	}

	fun setFilters(filters: FilterOptions) {
		adapter.filters = filters
		_filters.value = filters
	}

	fun setStartLetter(letter: String?) {
		adapter.startLetter = letter
	}

	fun getFilters(): FilterOptions? = filters.value

	fun getSortBy(): ItemSortBy? = sortBy.value

	fun getStartLetter(): String? = adapter.startLetter

	fun reRetrieveIfNeeded(): Boolean {
		return adapter.ReRetrieveIfNeeded()
	}

	fun refreshItem(item: BaseRowItem, onComplete: () -> Unit) {
		coroutineScope.launch {
			onComplete()
		}
	}

	companion object {
		fun createAdapter(
			context: Context,
			libraryPreferences: LibraryPreferences,
			cardPresenter: CardPresenter,
			rowDef: BrowseRowDef,
			chunkSize: Int
		): ItemRowAdapter {

			val adapter = when (rowDef.queryType) {
				QueryType.NextUp -> ItemRowAdapter(context, rowDef.nextUpQuery, true, cardPresenter, null)
				QueryType.Views -> ItemRowAdapter(context, GetUserViewsRequest, cardPresenter, null)
				QueryType.SimilarSeries -> ItemRowAdapter(context, rowDef.similarQuery, QueryType.SimilarSeries, cardPresenter, null)
				QueryType.SimilarMovies -> ItemRowAdapter(context, rowDef.similarQuery, QueryType.SimilarMovies, cardPresenter, null)
				QueryType.LiveTvChannel -> ItemRowAdapter(context, rowDef.tvChannelQuery, 40, cardPresenter, null)
				QueryType.LiveTvProgram -> ItemRowAdapter(context, rowDef.programQuery, cardPresenter, null);
				QueryType.LiveTvRecording -> ItemRowAdapter(context, rowDef.recordingQuery, chunkSize, cardPresenter, null);
				QueryType.Artists -> ItemRowAdapter(context, rowDef.artistsQuery, chunkSize, cardPresenter, null);
				QueryType.AlbumArtists -> ItemRowAdapter(context, rowDef.albumArtistsQuery, chunkSize, cardPresenter, null);
				else -> ItemRowAdapter(context, rowDef.query, chunkSize, rowDef.preferParentThumb, rowDef.isStaticHeight, cardPresenter, null);
			}

			val filters = FilterOptions().apply {
				isFavoriteOnly = libraryPreferences[LibraryPreferences.filterFavoritesOnly]
				isUnwatchedOnly = libraryPreferences[LibraryPreferences.filterUnwatchedOnly]
			}
			adapter.filters = filters

			val sortBy = libraryPreferences[LibraryPreferences.sortBy]
			val sortOrder = libraryPreferences[LibraryPreferences.sortOrder]
			adapter.setSortBy(BrowseGridFragment.SortOption("", sortBy, sortOrder))

			return adapter
		}
	}
}
