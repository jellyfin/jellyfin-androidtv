package org.jellyfin.androidtv.ui.home.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.ui.composable.tv.MediaCard
import org.jellyfin.androidtv.ui.composable.tv.MediaRow
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Enhanced Home screen Compose UI with multiple content sections
 */
@Composable
fun SimpleHomeScreen(
	homeState: HomeScreenState,
	viewModel: SimpleHomeViewModel,
	onLibraryClick: (BaseItemDto) -> Unit,
	onItemClick: (BaseItemDto) -> Unit = {},
	getItemImageUrl: (BaseItemDto) -> String? = { null },
	modifier: Modifier = Modifier,
) {
	when {
		homeState.isLoading -> {
			LoadingState(
				modifier = modifier.fillMaxSize(),
			)
		}
		homeState.error != null -> {
			ErrorState(
				error = homeState.error,
				modifier = modifier.fillMaxSize(),
			)
		}
		else -> {
			HomeContent(
				homeState = homeState,
				viewModel = viewModel,
				onLibraryClick = onLibraryClick,
				onItemClick = onItemClick,
				getItemImageUrl = getItemImageUrl,
				modifier = modifier.fillMaxSize(),
			)
		}
	}
}

@Composable
private fun HomeContent(
	homeState: HomeScreenState,
	viewModel: SimpleHomeViewModel,
	onLibraryClick: (BaseItemDto) -> Unit,
	onItemClick: (BaseItemDto) -> Unit,
	getItemImageUrl: (BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	LazyColumn(
		modifier = modifier,
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(24.dp),
	) {
		// Welcome header
		item {
			Column {
				Text(
					text = "Welcome back",
					style = MaterialTheme.typography.headlineLarge.copy(
						fontWeight = FontWeight.Bold,
					),
					color = MaterialTheme.colorScheme.onSurface,
				)
				Text(
					text = "Continue watching or discover something new",
					style = MaterialTheme.typography.bodyLarge,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(top = 4.dp),
				)
			}
		}

		// Continue Watching section
		if (homeState.resumeItems.isNotEmpty()) {
			item {
				ContentSection(
					title = "Continue Watching",
					items = homeState.resumeItems,
					onItemClick = onItemClick,
					getItemImageUrl = { item -> viewModel.getSeriesImageUrl(item) },
				)
			}
		}

		// Next Up section
		if (homeState.nextUpItems.isNotEmpty()) {
			item {
				ContentSection(
					title = "Next Up",
					items = homeState.nextUpItems,
					onItemClick = onItemClick,
					getItemImageUrl = { item -> viewModel.getSeriesImageUrl(item) },
				)
			}
		}

		// Your Libraries section
		if (homeState.libraries.isNotEmpty()) {
			item {
				LibrariesSection(
					libraries = homeState.libraries,
					onLibraryClick = onLibraryClick,
					getItemImageUrl = getItemImageUrl,
				)
			}
		}

		// Latest Movies section
		if (homeState.latestMovies.isNotEmpty()) {
			item {
				ContentSection(
					title = "Latest Movies",
					items = homeState.latestMovies,
					onItemClick = onItemClick,
					getItemImageUrl = getItemImageUrl,
				)
			}
		}

		// Latest Episodes section
		if (homeState.latestEpisodes.isNotEmpty()) {
			item {
				ContentSection(
					title = "Latest Episodes",
					items = homeState.latestEpisodes,
					onItemClick = onItemClick,
					getItemImageUrl = { item -> viewModel.getSeriesImageUrl(item) },
				)
			}
		}

		// Recently Added Stuff section
		if (homeState.recentlyAddedStuff.isNotEmpty()) {
			item {
				ContentSection(
					title = "Recently Added",
					items = homeState.recentlyAddedStuff,
					onItemClick = onItemClick,
					getItemImageUrl = getItemImageUrl,
				)
			}
		}
	}
}

@Composable
private fun ContentSection(
	title: String,
	items: List<BaseItemDto>,
	onItemClick: (BaseItemDto) -> Unit,
	getItemImageUrl: (BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	MediaRow(
		title = title,
		items = items,
		onItemClick = onItemClick,
		getItemImageUrl = getItemImageUrl,
		modifier = modifier,
	)
}

@Composable
private fun LibrariesSection(
	libraries: List<BaseItemDto>,
	onLibraryClick: (BaseItemDto) -> Unit,
	getItemImageUrl: (BaseItemDto) -> String?,
	modifier: Modifier = Modifier,
) {
	Column(modifier = modifier) {
		Text(
			text = "Your Libraries",
			style = MaterialTheme.typography.headlineSmall.copy(
				fontWeight = FontWeight.SemiBold,
			),
			color = MaterialTheme.colorScheme.onSurface,
			modifier = Modifier.padding(bottom = 12.dp),
		)

		LazyRow(
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
		) {
			items(libraries) { library ->
				HorizontalLibraryCard(
					item = library,
					imageUrl = getItemImageUrl(library),
					onClick = { onLibraryClick(library) },
				)
			}
		}
	}
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HorizontalLibraryCard(
	item: BaseItemDto,
	imageUrl: String?,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	var isFocused by remember { mutableStateOf(false) }
	
	androidx.tv.material3.Card(
		onClick = onClick,
		modifier = modifier
			.width(280.dp)
			.height(157.dp)
			.onFocusChanged { isFocused = it.isFocused },
		shape = androidx.tv.material3.CardDefaults.shape(
			shape = RoundedCornerShape(12.dp)
		),
		colors = androidx.tv.material3.CardDefaults.colors(
			containerColor = if (isFocused) MaterialTheme.colorScheme.surfaceVariant
			else MaterialTheme.colorScheme.surface,
		),
		scale = androidx.tv.material3.CardDefaults.scale(
			scale = if (isFocused) 1.05f else 1.0f,
		),
		border = androidx.tv.material3.CardDefaults.border(
			focusedBorder = androidx.tv.material3.Border(
				border = androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
				shape = RoundedCornerShape(12.dp),
			),
		),
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.clip(RoundedCornerShape(12.dp))
		) {
			AsyncImage(
				model = imageUrl,
				contentDescription = item.name,
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop,
			)
			
			// Gradient overlay for text readability
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(
							colors = listOf(
								Color.Transparent,
								Color.Black.copy(alpha = 0.7f)
							),
							startY = 0f,
							endY = Float.POSITIVE_INFINITY
						)
					)
			)
			
			// Library name
			Text(
				text = item.name ?: "Unknown",
				style = MaterialTheme.typography.titleLarge.copy(
					fontWeight = FontWeight.Bold,
				),
				color = Color.White,
				modifier = Modifier
					.align(Alignment.BottomStart)
					.padding(16.dp),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
	}
}

@Composable
private fun LoadingState(
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			CircularProgressIndicator(
				color = MaterialTheme.colorScheme.primary,
			)
			Text(
				text = "Loading your content...",
				style = MaterialTheme.typography.bodyLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
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
		modifier = modifier,
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = "Error: $error",
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.error,
			textAlign = TextAlign.Center,
			modifier = Modifier.padding(16.dp),
		)
	}
}
