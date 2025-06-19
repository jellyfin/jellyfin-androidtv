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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.composable.tv.MultiSectionImmersiveList
import org.koin.androidx.compose.koinViewModel

/**
 * Compose TV Shows screen with immersive list layout
 * Supports navigation: TV Shows → Seasons → Episodes
 */
@Composable
fun ComposeTvShowsScreen(
	folderArguments: android.os.Bundle?,
	modifier: Modifier = Modifier,
	viewModel: ComposeTvShowsViewModel = koinViewModel(),
) {
	val uiState by viewModel.uiState.collectAsState()

	// Load data when the screen is first displayed
	LaunchedEffect(folderArguments) {
		viewModel.loadTvShowsData(folderArguments)
	}

	when {
		uiState.isLoading -> {
			LoadingScreen(modifier = modifier)
		}
		uiState.error != null -> {
			ErrorScreen(
				error = uiState.error!!,
				modifier = modifier,
			)
		}
		uiState.sections.isEmpty() -> {
			EmptyScreen(
				title = uiState.title,
				modifier = modifier,
			)
		}
		else -> {
			TvShowsContentScreen(
				uiState = uiState,
				viewModel = viewModel,
				modifier = modifier,
			)
		}
	}
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			CircularProgressIndicator(
				color = Color.White,
				modifier = Modifier.padding(bottom = 16.dp),
			)
			Text(
				text = "Loading TV Shows...",
				style = MaterialTheme.typography.bodyLarge,
				color = Color.White,
			)
		}
	}
}

@Composable
private fun ErrorScreen(
	error: String,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = "Error Loading TV Shows",
				style = MaterialTheme.typography.headlineMedium.copy(
					fontWeight = FontWeight.Bold,
				),
				color = Color.White,
				modifier = Modifier.padding(bottom = 8.dp),
			)
			Text(
				text = error,
				style = MaterialTheme.typography.bodyMedium,
				color = Color.White.copy(alpha = 0.8f),
				textAlign = TextAlign.Center,
				modifier = Modifier.padding(horizontal = 32.dp),
			)
		}
	}
}

@Composable
private fun EmptyScreen(
	title: String,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.headlineLarge.copy(
					fontWeight = FontWeight.Bold,
					fontSize = 32.sp,
				),
				color = Color.White,
				modifier = Modifier.padding(bottom = 16.dp),
			)
			Text(
				text = "No TV shows found in this library",
				style = MaterialTheme.typography.bodyLarge,
				color = Color.White.copy(alpha = 0.8f),
				textAlign = TextAlign.Center,
			)
		}
	}
}

@Composable
private fun TvShowsContentScreen(
	uiState: TvShowsUiState,
	viewModel: ComposeTvShowsViewModel,
	modifier: Modifier = Modifier,
) {
	MultiSectionImmersiveList(
		sections = uiState.sections,
		onItemClick = { item ->
			viewModel.onItemClick(item)
		},
		onItemFocus = { item ->
			viewModel.onItemFocus(item)
		},
		getItemImageUrl = { item ->
			viewModel.getItemImageUrl(item)
		},
		getItemBackdropUrl = { item ->
			viewModel.getItemBackdropUrl(item)
		},
		getItemLogoUrl = { item ->
			viewModel.getItemLogoUrl(item)
		},
		modifier = modifier.fillMaxSize(),
	)
}
