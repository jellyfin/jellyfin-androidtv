package org.jellyfin.androidtv.ui.home.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.composable.tv.MediaCard
import org.jellyfin.androidtv.ui.composable.tv.MediaRow
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Enhanced Home screen Compose UI with multiple content sections
 */
@Composable
fun SimpleHomeScreen(
    homeState: HomeScreenState,
    onLibraryClick: (BaseItemDto) -> Unit,
    onItemClick: (BaseItemDto) -> Unit = {},
    getItemImageUrl: (BaseItemDto) -> String? = { null },
    modifier: Modifier = Modifier
) {
    when {
        homeState.isLoading -> {
            LoadingState(
                modifier = modifier.fillMaxSize()
            )
        }
        homeState.error != null -> {
            ErrorState(
                error = homeState.error,
                modifier = modifier.fillMaxSize()
            )
        }
        else -> {                HomeContent(
                    homeState = homeState,
                    onLibraryClick = onLibraryClick,
                    onItemClick = onItemClick,
                    getItemImageUrl = getItemImageUrl,
                    modifier = modifier.fillMaxSize()
                )
        }
    }
}

@Composable
private fun HomeContent(
    homeState: HomeScreenState,
    onLibraryClick: (BaseItemDto) -> Unit,
    onItemClick: (BaseItemDto) -> Unit,
    getItemImageUrl: (BaseItemDto) -> String?,
    modifier: Modifier = Modifier
	homeState: HomeScreenState,
	onLibraryClick: (BaseItemDto) -> Unit,
	onItemClick: (BaseItemDto) -> Unit,
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
            item {                ContentSection(
                    title = "Continue Watching",
                    items = homeState.resumeItems,
                    onItemClick = onItemClick,
                    getItemImageUrl = getItemImageUrl
                )
            }
        }        // Next Up section  
        if (homeState.nextUpItems.isNotEmpty()) {
            item {
                ContentSection(
                    title = "Next Up",
                    items = homeState.nextUpItems,
                    onItemClick = onItemClick,
                    getItemImageUrl = getItemImageUrl
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
                    getItemImageUrl = getItemImageUrl
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
                    getItemImageUrl = getItemImageUrl
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
				)
			}
		}

		// Your Libraries section
		if (homeState.libraries.isNotEmpty()) {
			item {
				LibrariesSection(
					libraries = homeState.libraries,
					onLibraryClick = onLibraryClick,
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        MediaRow(
            title = title,
            items = items,
            onItemClick = onItemClick,
            getItemImageUrl = getItemImageUrl
        )
    }
	title: String,
	items: List<BaseItemDto>,
	onItemClick: (BaseItemDto) -> Unit,
	modifier: Modifier = Modifier,
) {
	Column(modifier = modifier) {
		Text(
			text = title,
			style = MaterialTheme.typography.headlineSmall.copy(
				fontWeight = FontWeight.SemiBold,
			),
			color = MaterialTheme.colorScheme.onSurface,
			modifier = Modifier.padding(bottom = 12.dp),
		)
		MediaRow(
			title = title,
			items = items,
			onItemClick = onItemClick,
		)
	}
}

@Composable
private fun LibrariesSection(
	libraries: List<BaseItemDto>,
	onLibraryClick: (BaseItemDto) -> Unit,
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
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			contentPadding = PaddingValues(horizontal = 4.dp),
		) {
			items(libraries) { library ->
				MediaCard(
					item = library,
					onClick = { onLibraryClick(library) },
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

@Composable
private fun LibrariesContent(
	libraries: List<BaseItemDto>,
	onLibraryClick: (BaseItemDto) -> Unit,
	modifier: Modifier = Modifier,
) {
	if (libraries.isEmpty()) {
		Box(
			modifier = modifier,
			contentAlignment = Alignment.Center,
		) {
			Text(
				text = "No libraries found",
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.padding(16.dp),
			)
		}
	} else {
		LazyColumn(
			modifier = modifier,
			contentPadding = PaddingValues(16.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item {
				Text(
					text = "Your Libraries",
					style = MaterialTheme.typography.headlineSmall,
					modifier = Modifier.padding(bottom = 8.dp),
				)
			}
			items(libraries) { library ->
				MediaCard(
					item = library,
					onClick = { onLibraryClick(library) },
					modifier = Modifier.fillMaxWidth(),
				)
			}
		}
	}
}
