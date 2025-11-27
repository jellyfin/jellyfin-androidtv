package org.jellyfin.androidtv.ui.browsing.grid

import android.content.Context
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.androidtv.data.model.FilterOptions
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.ui.browsing.BrowseGridFragment
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.browsing.BrowsingUtils
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapterWrapper
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

data class SortOption(
	val name: String,
	val value: ItemSortBy,
	val order: SortOrder
)

class BrowseGridViewModelFactory(
	private val context: Context,
	private val folder: BaseItemDto,
) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(BrowseGridViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return BrowseGridViewModel(
				context,
				folder
			) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}

class BrowseGridViewModel(
	private val context: Context,
	folder: BaseItemDto,
) : ViewModel(), KoinComponent {
    private val _folder = MutableStateFlow(folder)
    val folder: StateFlow<BaseItemDto> = _folder.asStateFlow()

    private val preferencesRepository: PreferencesRepository by inject()
	private val userViewsRepository: UserViewsRepository by inject()
	private val itemLauncher: ItemLauncher by inject()
	private val backgroundService: BackgroundService by inject()

	private var adapterWrapper: ItemRowAdapterWrapper? = null
	private lateinit var adapter: ItemRowAdapter

	private val libraryPreferences = preferencesRepository.getLibraryPreferences(folder.displayPreferencesId ?: "empty_preferences")

	private var backgroundUpdateJob: Job? = null
	companion object {
		private const val VIEW_SELECT_UPDATE_DELAY = 250L
	}

	private val _allowViewSelection = MutableStateFlow(userViewsRepository.allowViewSelection(folder.collectionType))
	val allowViewSelection: StateFlow<Boolean> = _allowViewSelection.asStateFlow()

	private val _posterSize = MutableStateFlow(libraryPreferences[LibraryPreferences.posterSize])
	val posterSize: StateFlow<PosterSize> = _posterSize.asStateFlow()

	private val _imageType = MutableStateFlow(libraryPreferences[LibraryPreferences.imageType])
	val imageType: StateFlow<ImageType> = _imageType.asStateFlow()

	private val _gridDirection = MutableStateFlow(libraryPreferences[LibraryPreferences.gridDirection])
	val gridDirection: StateFlow<GridDirection> = _gridDirection.asStateFlow()

	private  val _filterFavoritesOnly = MutableStateFlow(libraryPreferences[LibraryPreferences.filterFavoritesOnly])
	val filterFavoritesOnly: StateFlow<Boolean> = _filterFavoritesOnly.asStateFlow()

	private  val _filterUnwatchedOnly = MutableStateFlow(libraryPreferences[LibraryPreferences.filterUnwatchedOnly])
	val filterUnwatchedOnly: StateFlow<Boolean> = _filterUnwatchedOnly.asStateFlow()

	private val _sortBy = MutableStateFlow(libraryPreferences[LibraryPreferences.sortBy])
	val sortBy: StateFlow<ItemSortBy?> = _sortBy.asStateFlow()

	private val _items = MutableStateFlow<List<BaseRowItem>>(emptyList())
	val items: StateFlow<List<BaseRowItem>> = _items.asStateFlow()

	private val _isLoading = MutableStateFlow(false)
	val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

	private val _totalItems = MutableStateFlow(0)
	val totalItems: StateFlow<Int> = _totalItems.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex = _selectedIndex.asStateFlow()

	private val _filters = MutableStateFlow<FilterOptions?>(null)
	val filters: StateFlow<FilterOptions?> = _filters.asStateFlow()

	private val _startLetter = MutableStateFlow<String?>(null)
	val startLetter: StateFlow<String?> = _startLetter.asStateFlow()

	private val _sortOptions = MutableStateFlow<Map<Int, SortOption>>(emptyMap())
	val sortOptions: StateFlow<Map<Int, SortOption>> = _sortOptions.asStateFlow()

	private val _imageSize = MutableStateFlow<IntSize>(IntSize(100,150))
	val imageSize: StateFlow<IntSize> = _imageSize.asStateFlow()

	init {

		initializeAdapter()
		initializeAdapterWrapper()
		initializeSortOptions()

	}

	fun initializeAdapter(){
		val chunkSize = 100
		val cardHeight = 150
		val cardPresenter = CardPresenter(false, imageType.value, cardHeight)
		val rowDef = BrowseRowDef("", BrowsingUtils.createBrowseGridItemsRequest(folder.value), chunkSize, false, true)
		cardPresenter.setUniformAspect(true);

		adapter = ItemRowAdapterWrapper.createAdapter(context, libraryPreferences, cardPresenter, rowDef, chunkSize)
	}


	fun initializeAdapterWrapper() {

		adapterWrapper = ItemRowAdapterWrapper(adapter, viewModelScope)

		viewModelScope.launch {
			adapterWrapper?.items?.collect {
				_items.value = it
				Timber.d("Updated items: ${it.size} items in viewModel")
			}
		}
		viewModelScope.launch {
			adapterWrapper?.isLoading?.collect {
				_isLoading.value = it
			}
		}
		viewModelScope.launch {
			adapterWrapper?.totalItems?.collect {
				_totalItems.value = it
			}
		}
		viewModelScope.launch {
			adapterWrapper?.filters?.collect {
				_filters.value = it
			}
		}
		viewModelScope.launch {
			adapterWrapper?.startLetter?.collect {
				_startLetter.value = it
			}
		}
		viewModelScope.launch {
			adapterWrapper?.sortBy?.collect {
				if (it != null) {
					_sortBy.value = it
				}
			}
		}

		if (_items.value.isEmpty()) {
			adapterWrapper?.retrieve()
		}
	}

	fun setRetrieveListener(lifecycle: Lifecycle) {
		adapterWrapper?.setRetrieveListener(lifecycle)
	}

    fun refreshPreferences() {
        _posterSize.value = libraryPreferences[LibraryPreferences.posterSize]
        _imageType.value = libraryPreferences[LibraryPreferences.imageType]
		_gridDirection.value = libraryPreferences[LibraryPreferences.gridDirection]
    }

	fun toggleFavoriteFilter() {
		viewModelScope.launch {
			val newValue = !filterFavoritesOnly.value
			libraryPreferences[LibraryPreferences.filterFavoritesOnly] = newValue
			libraryPreferences.commit()
			adapterWrapper?.setFilters(
				FilterOptions().apply {
					isFavoriteOnly = newValue
					isUnwatchedOnly = filterUnwatchedOnly.value
				}
			)
			adapterWrapper?.retrieve()
			_filterFavoritesOnly.value = newValue
		}
	}

	fun toggleUnwatchedOnly() {
		viewModelScope.launch {
			val newValue = !filterUnwatchedOnly.value
			libraryPreferences[LibraryPreferences.filterUnwatchedOnly] = newValue
			libraryPreferences.commit()
			adapterWrapper?.setFilters(
				FilterOptions().apply {
					isFavoriteOnly = filterFavoritesOnly.value
					isUnwatchedOnly = newValue
				}
			)
			adapterWrapper?.retrieve()
			_filterUnwatchedOnly.value = newValue
		}
	}

	fun setSelectedIndex(index: Int) {
        _selectedIndex.value = index

		if (index >= 0 && index < items.value.size) {
			val selectedItem = items.value[index]
			onItemSelected(selectedItem)
		} else {
			onItemDeselected()
		}
    }

	fun setSortBy(option: SortOption) {
		viewModelScope.launch {
			adapterWrapper?.setSortBy(BrowseGridFragment.SortOption(option.name, option.value, option.order))
			adapterWrapper?.retrieve()
		}
	}

	fun setStartLetter(letter: String?) {
		viewModelScope.launch {
			adapterWrapper?.setStartLetter(letter)
			adapterWrapper?.retrieve()
		}
	}

	fun loadMoreItemsIfNeeded(position: Int) {
		Timber.d("loadMoreItemsIfNeeded called for position $position")
		adapterWrapper?.loadMoreItemsIfNeeded(position)
	}

	fun onCardClicked(item: BaseRowItem) {
		itemLauncher.launch(item, adapter, context)
	}

	fun setImageSize(size: IntSize) {
		_imageSize.value = size
	}

	private fun initializeSortOptions() {

		val sortOptions = mutableMapOf(
			0 to SortOption(context.getString(R.string.lbl_name), ItemSortBy.SORT_NAME, SortOrder.ASCENDING),
			1 to SortOption(context.getString(R.string.lbl_date_added), ItemSortBy.DATE_CREATED, SortOrder.DESCENDING),
			2 to SortOption(context.getString(R.string.lbl_premier_date), ItemSortBy.PREMIERE_DATE, SortOrder.DESCENDING),
			3 to SortOption(context.getString(R.string.lbl_rating), ItemSortBy.OFFICIAL_RATING, SortOrder.ASCENDING),
			4 to SortOption(context.getString(R.string.lbl_community_rating), ItemSortBy.COMMUNITY_RATING, SortOrder.DESCENDING),
			5 to SortOption(context.getString(R.string.lbl_critic_rating), ItemSortBy.CRITIC_RATING, SortOrder.DESCENDING)
		)

		if (folder.value.collectionType == CollectionType.TVSHOWS) {
			sortOptions[6] = SortOption(context.getString(R.string.lbl_last_played), ItemSortBy.SERIES_DATE_PLAYED, SortOrder.DESCENDING)
		} else {
			sortOptions[6] = SortOption(context.getString(R.string.lbl_last_played), ItemSortBy.DATE_PLAYED, SortOrder.DESCENDING)
		}

		if (folder.value.collectionType == CollectionType.MOVIES) {
			sortOptions[7] = SortOption(context.getString(R.string.lbl_runtime), ItemSortBy.RUNTIME, SortOrder.ASCENDING)
		}

		_sortOptions.value = sortOptions
	}

	private fun onItemSelected(item: BaseRowItem) {
		backgroundUpdateJob?.cancel()
		backgroundUpdateJob = viewModelScope.launch {
			delay(VIEW_SELECT_UPDATE_DELAY)
			backgroundService.setBackground(item.baseItem)
		}
	}

	private fun onItemDeselected() {
		backgroundUpdateJob?.cancel()
		backgroundService.clearBackgrounds()
	}

	override fun onCleared() {
		super.onCleared()
		backgroundUpdateJob?.cancel()
		backgroundService.clearBackgrounds()
	}
}


