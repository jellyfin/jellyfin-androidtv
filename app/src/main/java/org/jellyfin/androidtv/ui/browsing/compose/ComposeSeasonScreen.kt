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
import org.jellyfin.androidtv.ui.composable.tv.MultiSectionImmersiveList
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

/**
 * Season detail screen showing episodes using the immersive list pattern
 * This screen displays all episodes for a specific season
 */
@Composable
fun ComposeSeasonScreen(
	seasonId: UUID,
	modifier: Modifier = Modifier,
	viewModel: ComposeSeasonViewModel = koinViewModel(),
) {
	val uiState by viewModel.uiState.collectAsState()

	// Load season data when the screen is first composed
	LaunchedEffect(seasonId) {
		viewModel.loadSeasonData(seasonId)
	}

	Box(
		modifier = modifier.fillMaxSize(),
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
					getItemLogoUrl = viewModel::getItemLogoUrl,
				)
			}
		}
	}
}

@Composable
private fun LoadingState(
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			CircularProgressIndicator()
			Text(
				text = "Loading episodes...",
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.padding(top = 16.dp),
			)
		}
	}
}

@Composable
private fun ErrorState(
	error: String,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = "Error: $error",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.error,
		)
	}
}

@Composable
private fun EmptyState(
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = "No episodes found",
			style = MaterialTheme.typography.bodyLarge,
		)
	}
}

@Composable
private fun ContentState(
	uiState: SeasonUiState,
	onItemClick: (org.jellyfin.sdk.model.api.BaseItemDto) -> Unit,
	onItemFocused: (org.jellyfin.sdk.model.api.BaseItemDto?) -> Unit,
	getItemImageUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	getItemBackdropUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	getItemLogoUrl: (org.jellyfin.sdk.model.api.BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	MultiSectionImmersiveList(
		sections = uiState.sections,
		onItemClick = onItemClick,
		onItemFocus = onItemFocused,
		getItemImageUrl = getItemImageUrl,
		getItemBackdropUrl = getItemBackdropUrl,
		getItemLogoUrl = getItemLogoUrl,
		modifier = modifier.fillMaxSize(),
	)
}

private fun buildTitle(uiState: SeasonUiState): String {
	return when {
		uiState.seriesName != null && uiState.title != "Season" -> {
			"${uiState.seriesName} - ${uiState.title}"
		}
		uiState.seriesName != null -> {
			uiState.seriesName
		}
		else -> uiState.title
	}
}
