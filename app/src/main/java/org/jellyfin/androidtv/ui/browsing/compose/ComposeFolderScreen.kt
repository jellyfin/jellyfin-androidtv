package org.jellyfin.androidtv.ui.browsing.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.browsing.MediaSection
import org.jellyfin.androidtv.ui.composable.tv.MediaRow
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun ComposeFolderScreen(
	viewModel: ComposeFolderViewModel,
	folder: BaseItemDto,
	onItemClick: (BaseItemDto) -> Unit,
	modifier: Modifier = Modifier,
) {
	val uiState by viewModel.uiState.collectAsState()

	LaunchedEffect(folder.id) {
		viewModel.loadFolderContent(folder)
	}

	when {
		uiState.isLoading -> {
			LoadingScreen(modifier = modifier)
		} uiState.error != null -> {
			ErrorScreen(
				error = uiState.error!!, // Non-null assertion since we checked it above
				modifier = modifier,
			)
		}
		else -> {
			FolderContentScreen(
				folderName = uiState.folderName,
				sections = uiState.sections,
				onItemClick = onItemClick,
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
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			CircularProgressIndicator()
			Text(
				text = stringResource(R.string.loading),
				style = MaterialTheme.typography.bodyLarge,
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
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			Text(
				text = "Error", // Using hardcoded string since no loading_error string resource exists
				style = MaterialTheme.typography.headlineSmall,
				color = MaterialTheme.colorScheme.error,
			)
			Text(
				text = error,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
private fun FolderContentScreen(
	folderName: String,
	sections: List<MediaSection>,
	onItemClick: (BaseItemDto) -> Unit,
	modifier: Modifier = Modifier,
) {
	LazyColumn(
		modifier = modifier.fillMaxSize(),
		verticalArrangement = Arrangement.spacedBy(24.dp),
		contentPadding = androidx.compose.foundation.layout.PaddingValues(
			horizontal = 48.dp,
			vertical = 32.dp,
		),
	) {
		item {
			Text(
				text = folderName,
				style = MaterialTheme.typography.headlineLarge,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onSurface,
			)
		}

		items(sections) { section ->
			MediaSectionComposable(
				section = section,
				onItemClick = onItemClick,
			)
		}
	}
}

@Composable
private fun MediaSectionComposable(
	section: MediaSection,
	onItemClick: (BaseItemDto) -> Unit,
	modifier: Modifier = Modifier,
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(16.dp),
	) {
		Text(
			text = section.title,
			style = MaterialTheme.typography.headlineSmall,
			fontWeight = FontWeight.SemiBold,
			color = MaterialTheme.colorScheme.onSurface,
		)
		MediaRow(
			title = section.title,
			items = section.items,
			onItemClick = onItemClick,
		)
	}
}
