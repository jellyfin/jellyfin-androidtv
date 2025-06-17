package org.jellyfin.androidtv.ui.browsing.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import org.jellyfin.androidtv.ui.composable.tv.MediaBrowseLayout
import org.jellyfin.androidtv.ui.composable.tv.MediaSection
import org.jellyfin.androidtv.ui.theme.JellyfinTvTheme
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.androidx.compose.koinViewModel

/**
 * Compose version of the home browsing experience
 * This demonstrates migrating from Leanback BrowseFragment to Compose TV
 */
@Composable
fun HomeBrowseScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: HomeBrowseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    JellyfinTvTheme {
        when {
            uiState.isLoading -> {
                // Loading state - could show skeleton
                HomeBrowseLoadingScreen()
            }
            uiState.sections.isNotEmpty() -> {
                MediaBrowseLayout(
                    sections = uiState.sections,
                    onItemClick = { item ->
                        viewModel.onItemClick(item, navController)
                    },
                    onItemFocus = { item ->
                        viewModel.onItemFocus(item)
                    },
                    getItemImageUrl = { item ->
                        viewModel.getItemImageUrl(item)
                    },
                    modifier = modifier.fillMaxSize()
                )
            }
            else -> {
                // Empty state
                HomeBrowseEmptyScreen()
            }
        }
    }
}

@Composable
private fun HomeBrowseLoadingScreen() {
    // TODO: Implement loading skeleton
}

@Composable
private fun HomeBrowseEmptyScreen() {
    // TODO: Implement empty state
}

/**
 * Data class representing the UI state
 */
data class HomeBrowseUiState(
    val isLoading: Boolean = true,
    val sections: List<MediaSection> = emptyList(),
    val error: String? = null,
    val focusedItem: BaseItemDto? = null
)
