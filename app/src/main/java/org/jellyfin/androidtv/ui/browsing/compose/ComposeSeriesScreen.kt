package org.jellyfin.androidtv.ui.browsing.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.composable.layout.MultiSectionImmersiveList
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

/**
 * Series detail screen showing seasons using the immersive list pattern
 * This screen displays all seasons for a specific TV series
 */
@Composable
fun ComposeSeriesScreen(
	seriesId: UUID,
	modifier: Modifier = Modifier,
	viewModel: ComposeSeriesViewModel = koinViewModel()
) {
	val uiState by viewModel.uiState.collectAsState()

	// Load series data when the screen is first composed
	LaunchedEffect(seriesId) {
		viewModel.loadSeriesData(seriesId)
	}

	Box(
		modifier = modifier.fillMaxSize()
	) {
		when {
			uiState.isLoading -> {
				LoadingState()
			}
			uiState.error != null -> {
				ErrorState(error = uiState.error)
			}
			uiState.sections.isEmpty() -> {
				EmptyState()
			}
			else -> {
				ContentState(
					uiState = uiState,
					onItemClick = viewModel::onItemClick,
					onItemFocused = viewModel::onItemFocused,
					getItemImageUrl = viewModel::getItemImageUrl,
					getItemBackdropUrl = viewModel::getItemBackdropUrl,
					getItemLogoUrl = viewModel::getItemLogoUrl
				)
			}
		}
	}
}

@Composable
private fun LoadingState(
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			CircularProgressIndicator()
			Text(
				text = "Loading seasons...",
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.padding(top = 16.dp)
			)
		}
	}
}

@Composable
private fun ErrorState(
	error: String,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = "Error: $error",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.error
		)
	}
}

@Composable
private fun EmptyState(
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = "No seasons found",
			style = MaterialTheme.typography.bodyLarge
		)
	}
}

@Composable
private fun ContentState(
	uiState: SeriesUiState,
	onItemClick: (org.jellyfin.sdk.model.api.BaseItemDto) -> Unit,
	onItemFocused: (org.jellyfin.sdk.model.api.BaseItemDto?) -> Unit,
	getItemImageUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	getItemBackdropUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	getItemLogoUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	modifier: Modifier = Modifier
) {
	MultiSectionImmersiveList(
		sections = uiState.sections,
		title = uiState.title,
		backgroundImageUrl = uiState.focusedItem?.let { getItemBackdropUrl(it) } 
			?: uiState.series?.let { getItemBackdropUrl(it) },
		logoImageUrl = uiState.focusedItem?.let { getItemLogoUrl(it) } 
			?: uiState.series?.let { getItemLogoUrl(it) },
		onItemClick = onItemClick,
		onItemFocused = onItemFocused,
		getItemImageUrl = getItemImageUrl,
		modifier = modifier
	)
}
