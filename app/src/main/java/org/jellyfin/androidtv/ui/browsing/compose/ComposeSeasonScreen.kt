package org.jellyfin.androidtv.ui.browsing.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.composable.tv.MultiSectionImmersiveList
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

/**
 * Season detail screen showing episodes using the immersive list pattern
 * This screen displays all episodes for a specific season with series context
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
				ErrorState(error = uiState.error!!)
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
	Box(modifier = modifier.fillMaxSize()) {
		// Main content
		MultiSectionImmersiveList(
			sections = uiState.sections,
			onItemClick = onItemClick,
			onItemFocus = { item -> onItemFocused(item) },
			getItemImageUrl = getItemImageUrl,
			getItemBackdropUrl = getItemBackdropUrl,
			getItemLogoUrl = getItemLogoUrl,
			modifier = Modifier.fillMaxSize(),
		)
		
		// Season header overlay
		SeasonHeaderOverlay(
			uiState = uiState,
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.TopStart),
		)
	}
}

@Composable
private fun SeasonHeaderOverlay(
	uiState: SeasonUiState,
	modifier: Modifier = Modifier,
) {
	val title = buildTitle(uiState)
	val episodeCount = uiState.sections
		.find { it.title == "Episodes" }
		?.items?.size ?: 0
	
	Column(
		modifier = modifier
			.background(
				Brush.verticalGradient(
					colors = listOf(
						Color.Black.copy(alpha = 0.8f),
						Color.Black.copy(alpha = 0.6f),
						Color.Transparent,
					),
				),
			)
			.padding(
				start = 48.dp,
				top = 32.dp,
				end = 48.dp,
				bottom = 24.dp,
			),
	) {
		Text(
			text = title,
			style = MaterialTheme.typography.headlineLarge,
			color = Color.White,
			fontWeight = FontWeight.Bold,
		)
		
		if (episodeCount > 0) {
			Spacer(modifier = Modifier.height(8.dp))
			Row(
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = "$episodeCount episodes",
					style = MaterialTheme.typography.bodyLarge,
					color = Color.White.copy(alpha = 0.8f),
				)
				
				// Show season info if available
				uiState.season?.let { season ->
					season.productionYear?.let { year ->
						Text(
							text = "•",
							style = MaterialTheme.typography.bodyLarge,
							color = Color.White.copy(alpha = 0.6f),
						)
						Text(
							text = year.toString(),
							style = MaterialTheme.typography.bodyLarge,
							color = Color.White.copy(alpha = 0.8f),
						)
					}
				}
			}
		}
	}
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
