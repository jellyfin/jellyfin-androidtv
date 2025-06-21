package org.jellyfin.androidtv.ui.browsing.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.composable.tv.ImmersiveBackground
import org.jellyfin.androidtv.ui.theme.JellyfinColors
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaStreamType
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import org.jellyfin.androidtv.util.formatShortDisplayTime

/**
 * Episode Detail screen showing information about a specific episode.
 * Features an immersive background.
 */
@Composable
fun ComposeEpisodeDetailScreen(
    episodeId: UUID,
    modifier: Modifier = Modifier,
    viewModel: ComposeEpisodeDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(episodeId) {
        viewModel.loadEpisodeDetails(episodeId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Immersive background for the entire screen
        ImmersiveBackground(
            item = uiState.episode,
            getBackdropUrl = { item -> viewModel.getItemBackdropUrl(item) }
        )

        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.align(Alignment.Center))
            uiState.error != null -> ErrorState(
                error = uiState.error!!,
                modifier = Modifier.align(Alignment.Center)
            )
            uiState.episode != null -> EpisodeContent(
                episode = uiState.episode!!,
                seriesName = uiState.seriesName,
                seasonName = uiState.seasonName,
                onPlayClick = { viewModel.playEpisode(uiState.episode!!) },
                onMarkWatchedClick = { viewModel.toggleWatchedStatus(uiState.episode!!) },
                getItemLogoUrl = { item -> viewModel.getItemLogoUrl(item) },
                modifier = Modifier.fillMaxSize()
            )
            // Handles the case where episode is null after loading and no error
            else -> EmptyState(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Color.White)
        Text(
            text = stringResource(R.string.compose_loading_episode_details),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ErrorState(error: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.compose_error_loading_episode, error),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.padding(32.dp)
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.compose_episode_not_found),
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White.copy(alpha = 0.8f),
        modifier = modifier.padding(32.dp)
    )
}

@Composable
private fun EpisodeContent(
    episode: BaseItemDto,
    seriesName: String?,
    seasonName: String?,
    onPlayClick: () -> Unit,
    onMarkWatchedClick: () -> Unit,
    getItemLogoUrl: (BaseItemDto) -> String?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Box(modifier = modifier) {
        // Content gradient overlay for text readability over the background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f), // Less dark at the top
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f), // Darker towards bottom for component contrast
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 32.dp)
                .verticalScroll(scrollState)
        ) {
            // Header: Series Name / Season Name
            if (!seriesName.isNullOrEmpty()) {
                Text(
                    text = seriesName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!seasonName.isNullOrEmpty()) {
                Text(
                    text = seasonName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = if (seriesName.isNullOrEmpty()) 0.dp else 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (seriesName.isNullOrEmpty() && seasonName.isNullOrEmpty()) 0.dp else 16.dp))

            // Episode Title
            val episodeTitle = episode.indexNumber?.let { index ->
                val seasonIndex = episode.parentIndexNumber
                if (seasonIndex != null && seasonIndex > 0) { // Display SxE format if season number is present
                    "S%02dE%02d. %s".format(seasonIndex, index, episode.name)
                } else {
                    "E$index. ${episode.name}"
                }
            } ?: episode.name ?: stringResource(R.string.unknown_episode)
            Text(
                text = episodeTitle,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Metadata Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp), // Reduced spacing
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                episode.productionYear?.let {
                    Text(it.toString(), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
                }
                episode.officialRating?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                }
                episode.runTimeTicks?.let { ticks ->
                    if (ticks > 0) {
                        val durationString = formatShortDisplayTime(context, ticks / 10000) // ticks to ms
                        Text(durationString, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
                    }
                }
                episode.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }?.height?.let { height ->
                    val quality = when {
                        height >= 2160 -> "4K UHD"
                        height >= 1080 -> "HD 1080p"
                        height >= 720 -> "HD 720p"
                        else -> "SD"
                    }
                    Text(quality, style = MaterialTheme.typography.labelMedium, color = JellyfinColors.Primary, fontWeight = FontWeight.SemiBold,
                         modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                Button(onClick = onPlayClick, colors = ButtonDefaults.buttonColors(containerColor = JellyfinColors.Primary)) {
                    Icon(painterResource(R.drawable.ic_play_arrow_white_24dp), contentDescription = stringResource(R.string.play_episode_description, episodeTitle), tint = Color.White)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.play).uppercase(), color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                val isWatched = episode.userData?.played == true
                val watchedButtonText = if (isWatched) R.string.action_mark_unwatched else R.string.action_mark_watched
                Button(onClick = onMarkWatchedClick, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))) {
                    Icon(
                        painter = painterResource(if (isWatched) R.drawable.ic_visibility_off_24dp else R.drawable.ic_visibility_24dp),
                        contentDescription = stringResource(watchedButtonText)
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(watchedButtonText).uppercase(), style = MaterialTheme.typography.labelLarge)
                }
            }

            // Overview
            episode.overview?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 20.dp) // Increased bottom padding
                )
            }

            // Premiere Date
            episode.premiereDate?.let { dateValue ->
                try {
                    val date = Date(dateValue.toEpochMilliseconds())
                    val formattedDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date)
                    Text(
                        text = stringResource(R.string.air_date_format, formattedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } catch (e: Exception) {
                    // Log error or handle, for now ignore date parsing errors
                }
            }

            // Community Rating
            episode.communityRating?.let { rating ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(painterResource(R.drawable.ic_star), contentDescription = stringResource(R.string.rating_content_description), tint = Color.Yellow, modifier = Modifier.size(20.dp))
                    Text(
                        text = " ${String.format("%.1f", rating)}/10",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            // TODO: Add more details as needed: Cast, Directors, Writers etc.
            // This would typically involve fetching more data or having it in the BaseItemDto via ItemRepository.itemFields
            // For example, a horizontal row of cast members if episode.people is populated.
        }

        // Series/Episode Logo (if available) - similar to ImmersiveList's ContentInformationOverlay
        val logoUrl = getItemLogoUrl(episode)
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = stringResource(R.string.logo_content_description, episode.name ?: seriesName ?: ""),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 48.dp) // Adjusted padding
                    .height(80.dp) // Adjusted size
                    .widthIn(max = 280.dp), // Adjusted size
                contentScale = ContentScale.Fit
            )
        }
    }
}

// Note: The actual ComposeEpisodeDetailViewModel and its UiState will be implemented in the next step.
// The placeholder data class and commented-out ViewModel in the original code were for compilation.
// The `org.jellyfin.androidtv.util.formatShortDisplayTime` is assumed to exist and handle time formatting.
// Ensure R.string resources like compose_loading_episode_details, compose_error_loading_episode, etc. are defined.
// Placeholder for R.string.play_episode_description, R.string.rating_content_description, R.string.logo_content_description
// These should be added to strings.xml for accessibility.
