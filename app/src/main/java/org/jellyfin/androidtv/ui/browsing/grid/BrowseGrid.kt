package org.jellyfin.androidtv.ui.browsing.grid

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.card.ImageCard
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.compose.koinInject
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.jellyfin.androidtv.ui.card.ImageCardMapper
import org.jellyfin.androidtv.ui.card.ImageCardUiState


@Composable
fun BrowseGrid(
	folder: BaseItemDto
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

	val viewModel: BrowseGridViewModel = viewModel(factory = BrowseGridViewModelFactory(context, folder))

    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
            viewModel.refreshPreferences()
    }

    val allowViewSelection by viewModel.allowViewSelection.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val posterSize by viewModel.posterSize.collectAsStateWithLifecycle()
    val imageType by viewModel.imageType.collectAsStateWithLifecycle()
    val gridDirection by viewModel.gridDirection.collectAsStateWithLifecycle()
    val filterFavoritesOnly by viewModel.filterFavoritesOnly.collectAsStateWithLifecycle()
    val filterUnwatchedOnly by viewModel.filterUnwatchedOnly.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()
    val totalItems by viewModel.totalItems.collectAsStateWithLifecycle()
    val startLetter by viewModel.startLetter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOptions.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()

	val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.setRetrieveListener(lifecycle)
    }

    Column(
        modifier = Modifier
			.fillMaxSize()
			.padding(horizontal = 24.dp)
    ) {
		Column(
			modifier = Modifier.weight(1f)
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 12.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.Bottom
			) {
				Text(text = folder.name ?: "", color = Color.White, fontSize = 32.sp)

				BrowseGridToolbar(
					sortOptions = sortOption.values.toList(),
					currentSortBy = sortBy ?: ItemSortBy.SORT_NAME,
					filterFavoritesOnly = filterFavoritesOnly,
					filterUnwatchedOnly = filterUnwatchedOnly,
					showUnwatchedFilter = true,
					showLetterJump = true,
					allowViewSelection = allowViewSelection,
					onSortSelected = { option ->
						viewModel.setSortBy(option)
					},
					onUnwatchedToggle = {
						viewModel.toggleUnwatchedOnly()
					},
					onFavoriteToggle = {
						viewModel.toggleFavoriteFilter()
					},
					onSettingsClick = {
						settingsLauncher.launch(
							ActivityDestinations.displayPreferences(
								context,
								folder.displayPreferencesId ?: "empty_preferences",
								allowViewSelection
							)
						)
					}
				)
			}

			Row() {
				BrowseGridAlphabetPanel(
					modifier = Modifier,
					selectedLetter = startLetter,
					onLetterSelected = { letter ->
						viewModel.setStartLetter(letter)
					}
				)
				when (gridDirection) {
					GridDirection.VERTICAL -> {
						VerticalBrowseGrid(
							items = items,
							posterSize = posterSize,
							imageType = imageType,
							focusRequester = focusRequester,
							onItemSelected = { index ->
								viewModel.setSelectedIndex(index)
							},
							viewModel = viewModel
						)
					}

					GridDirection.HORIZONTAL -> {
						HorizontalBrowseGrid(
							items = items,
							posterSize = posterSize,
							imageType = imageType,
							focusRequester = focusRequester,
							onItemSelected = { index ->
								viewModel.setSelectedIndex(index)
							},
							viewModel = viewModel
						)
					}
				}
			}

		}

        StatusBar(
			folderName = folder.name,
            filterFavoritesOnly = filterFavoritesOnly,
            filterUnwatchedOnly = filterUnwatchedOnly,
            focusedIndex = selectedIndex,
            totalItems = totalItems,
			startLetter = startLetter,
			sortBy = sortBy,
			sortOptions = sortOption
        )
    }
}

@Composable
private fun VerticalBrowseGrid(
	items: List<BaseRowItem>,
	posterSize: PosterSize,
	imageType: ImageType,
	focusRequester: FocusRequester,
	onItemSelected: (Int) -> Unit,
	viewModel: BrowseGridViewModel
) {
	val columns = calculateColumns(posterSize, imageType)
	val context = LocalContext.current
	val imageHelper = koinInject<ImageHelper>()
	val gridState = rememberLazyGridState()
	val imageCardMapper = remember { ImageCardMapper(context, imageHelper) }

	val imageSize by viewModel.imageSize.collectAsStateWithLifecycle()

	// Waiting for the first visible cell to set the focus
	LaunchedEffect(gridState) {
		snapshotFlow { gridState.layoutInfo.visibleItemsInfo.isNotEmpty() }
			.filter { it }
			.first()
		focusRequester.requestFocus()
	}

	// Tracking scrolling for pagination
	LaunchedEffect(gridState, items) {
		snapshotFlow {
			gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
		}.collect { lastVisibleItem ->
			if (items.isNotEmpty() && lastVisibleItem >= items.size - columns * 2) {
				viewModel.loadMoreItemsIfNeeded(lastVisibleItem)
			}
		}
	}

	LazyVerticalGrid(
		columns = GridCells.Fixed(columns),
		state = gridState,
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		modifier = Modifier
			.focusGroup()
			.focusRestorer()
			.focusRequester(focusRequester)
	) {
		itemsIndexed(items, key = { _, item -> item.itemId.toString()})
		{ index, item ->
			BrowseGridItem(
				modifier = Modifier,
				item = item,
				uiState = imageCardMapper.mapToUiState(item, imageType, imageSize),
				index = index,
				onItemSelected = onItemSelected,
				focusRequester = focusRequester,
				viewModel = viewModel
			)
		}
	}
}

@Composable
private fun HorizontalBrowseGrid(
	items: List<BaseRowItem>,
	posterSize: PosterSize,
	imageType: ImageType,
	focusRequester: FocusRequester,
	onItemSelected: (Int) -> Unit,
	viewModel: BrowseGridViewModel
) {
	val rows = calculateRows(posterSize, imageType)
	val context = LocalContext.current
	val imageHelper = koinInject<ImageHelper>()
	val gridState = rememberLazyGridState()
	val imageCardMapper = remember { ImageCardMapper(context, imageHelper) }

	val imageSize by viewModel.imageSize.collectAsStateWithLifecycle()

	// Waiting for the first visible cell to set the focus
	LaunchedEffect(gridState) {
		snapshotFlow { gridState.layoutInfo.visibleItemsInfo.isNotEmpty() }
			.filter { it }
			.first()
		focusRequester.requestFocus()
	}

	// Tracking scrolling for pagination
	LaunchedEffect(gridState, items) {
		snapshotFlow {
			gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
		}.collect { lastVisibleItem ->
			if (items.isNotEmpty() && lastVisibleItem >= items.size - rows * 2) {
				viewModel.loadMoreItemsIfNeeded(lastVisibleItem)
			}
		}
	}

	LazyHorizontalGrid(
		rows = GridCells.Fixed(rows),
		state = gridState,
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		modifier = Modifier
			.focusGroup()
			.focusRestorer()
			.focusRequester(focusRequester)
	) {
		itemsIndexed(items) { index, item ->
			BrowseGridItem(
				modifier = Modifier,
				item = item,
				uiState = imageCardMapper.mapToUiState(item, imageType, imageSize),
				index = index,
				onItemSelected = onItemSelected,
				focusRequester = focusRequester,
				viewModel = viewModel
			)
		}

	}
}

@Composable
private fun BrowseGridItem(
	modifier: Modifier,
	item: BaseRowItem,
	uiState: ImageCardUiState,
	index: Int,
	onItemSelected: (Int) -> Unit,
	focusRequester: FocusRequester,
	viewModel: BrowseGridViewModel
) {
	ImageCard(
		modifier =
			if (index == 0) {
				Modifier
					.onGloballyPositioned { coordinates ->
						viewModel.setImageSize(coordinates.size)
					}
			} else
				Modifier,
		mainImageUrl = uiState.imageUrl,
		defaultIconRes = uiState.defaultIconRes,
		overlayIconRes = uiState.overlayIconRes,
		overlayCount = uiState.overlayCount,
		aspectRatio = uiState.aspectRatio,
		title = uiState.title,
		contentText = uiState.contentText,
		unwatchedCount = uiState.unwatchedCount,
		progress = uiState.progress,
		isFavorite = uiState.isFavorite,
		onFocus = { hasFocus ->
			if (hasFocus) {
				onItemSelected(index)
			}
		},
		onClick = {
			focusRequester.saveFocusedChild()
			viewModel.onCardClicked(item)
		}
	)

}

@Composable
private fun StatusBar(
	folderName: String?,
	filterFavoritesOnly: Boolean,
	filterUnwatchedOnly: Boolean,
	startLetter: String? = null,
	sortBy: ItemSortBy? = null,
	sortOptions: Map<Int, SortOption> = emptyMap(),
	focusedIndex: Int = 0,
	totalItems: Int = 0,
) {

	val sortOptionNameUnknown = SortOption(stringResource(R.string.lbl_bracket_unknown), ItemSortBy.SORT_NAME, SortOrder.ASCENDING)
	val sortOptionName = sortOptions.values.find { it.value == sortBy } ?: sortOptionNameUnknown

    Row(
        modifier = Modifier
			.fillMaxWidth()
			.padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

		// Filter description
		Text(
			text = buildString {
				append(stringResource(R.string.lbl_showing))
				if (!filterFavoritesOnly && !filterUnwatchedOnly) append(" " + stringResource(R.string.lbl_all_items))
				if (filterUnwatchedOnly) append(" " + stringResource(R.string.lbl_unwatched))
				if (filterFavoritesOnly) append(" " + stringResource(R.string.lbl_favorites))
				if (startLetter != null) append(" " + stringResource(R.string.lbl_starting_with) + " " + startLetter)
				append(" " + stringResource(R.string.lbl_from) + " '" + folderName + "'")
				if (sortBy != null) append(" " + stringResource(R.string.lbl_sorted_by) + " " + sortOptionName.name)
			},
			color = Color.White,
			fontSize = 14.sp
		)

        // Counter
        Text(
            text = "${focusedIndex + 1}|$totalItems",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}


private fun calculateColumns(posterSize: PosterSize, imageType: ImageType): Int {
	return when (posterSize) {
		PosterSize.SMALLEST -> when (imageType) {
			ImageType.BANNER -> 6
			ImageType.THUMB -> 11
			else -> 15
		}
		PosterSize.SMALL -> when (imageType) {
			ImageType.BANNER -> 5
			ImageType.THUMB -> 9
			else -> 13
		}
		PosterSize.MED -> when (imageType) {
			ImageType.BANNER -> 4
			ImageType.THUMB -> 7
			else -> 11
		}
		PosterSize.LARGE -> when (imageType) {
			ImageType.BANNER -> 3
			ImageType.THUMB -> 5
			else -> 7
		}
		PosterSize.X_LARGE -> when (imageType) {
			ImageType.BANNER -> 2
			ImageType.THUMB -> 3
			else -> 5
		}
	}
}

private fun calculateRows(posterSize: PosterSize, imageType: ImageType): Int {
	return when (posterSize) {
		PosterSize.SMALLEST -> when (imageType) {
			ImageType.BANNER -> 13
			ImageType.THUMB -> 7
			else -> 5
		}
		PosterSize.SMALL -> when (imageType) {
			ImageType.BANNER -> 11
			ImageType.THUMB -> 6
			else -> 4
		}
		PosterSize.MED -> when (imageType) {
			ImageType.BANNER -> 9
			ImageType.THUMB -> 5
			else -> 3
		}
		PosterSize.LARGE -> when (imageType) {
			ImageType.BANNER -> 7
			ImageType.THUMB -> 4
			else -> 2
		}
		PosterSize.X_LARGE -> when (imageType) {
			ImageType.BANNER -> 5
			ImageType.THUMB -> 2
			else -> 1
		}
	}
}
