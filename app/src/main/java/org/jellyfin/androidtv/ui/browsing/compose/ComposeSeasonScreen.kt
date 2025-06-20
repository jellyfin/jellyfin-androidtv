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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveList
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

/**
 * TV Show seasons screen showing all seasons using the immersive list pattern
 * This screen displays all seasons for a specific TV show
 */
@Composable
fun ComposeSeasonScreen(
	seriesId: UUID,
	modifier: Modifier = Modifier,
	viewModel: ComposeSeasonViewModel = koinViewModel(),
) {
	val uiState by viewModel.uiState.collectAsState()

	// Load series seasons when the screen is first composed
	LaunchedEffect(seriesId) {
		viewModel.loadSeriesSeasons(seriesId)
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
				text = "Loading seasons...",
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
			text = "No seasons found",
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
		// Background banner image (clear, not blurred)
		uiState.series?.let { series ->
			getItemBackdropUrl(series)?.let { _ ->
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(Color.Black) // Fallback background
				) {
					// Background gradient overlay for text readability
					Box(
						modifier = Modifier
							.fillMaxSize()
							.background(
								Brush.verticalGradient(
									colors = listOf(
										Color.Black.copy(alpha = 0.3f),
										Color.Black.copy(alpha = 0.7f),
									),
								),
							)
					)
				}
			}
		}
		
		// Content sections
		LazyColumn(
			contentPadding = PaddingValues(
				top = 120.dp, // Space for header
				bottom = 32.dp,
			),
			verticalArrangement = Arrangement.spacedBy(32.dp),
			modifier = Modifier.fillMaxSize(),
		) {
			items(uiState.sections) { section ->
				ImmersiveList(
					title = section.title,
					items = section.items,
					layout = section.layout,
					onItemClick = onItemClick,
					onItemFocus = { item -> onItemFocused(item) },
					getItemImageUrl = getItemImageUrl,
					getItemBackdropUrl = getItemBackdropUrl,
					getItemLogoUrl = getItemLogoUrl,
					modifier = Modifier
						.fillMaxWidth()
						.height(280.dp),
				)
			}
		}
		
		// Series header overlay
		SeriesHeaderOverlay(
			uiState = uiState,
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.TopStart),
		)
	}
}

@Composable
private fun SeriesHeaderOverlay(
	uiState: SeasonUiState,
	modifier: Modifier = Modifier,
) {
	val title = buildTitle(uiState)
	val seasonCount = uiState.sections
		.find { it.title == "Seasons" }
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
		
		if (seasonCount > 0) {
			Spacer(modifier = Modifier.height(8.dp))
			Row(
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = "$seasonCount seasons",
					style = MaterialTheme.typography.bodyLarge,
					color = Color.White.copy(alpha = 0.8f),
				)
				
				// Show series info if available
				uiState.series?.let { series ->
					series.productionYear?.let { year ->
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
		uiState.seriesName != null -> {
			uiState.seriesName
		}
		else -> uiState.title
	}
}
