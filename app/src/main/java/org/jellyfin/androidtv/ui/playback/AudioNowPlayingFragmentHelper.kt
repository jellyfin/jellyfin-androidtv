package org.jellyfin.androidtv.ui.playback

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.ui.composable.LyricsDtoBox
import org.jellyfin.androidtv.ui.composable.modifier.fadingEdges
import org.jellyfin.androidtv.ui.composable.rememberPlayerProgress
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.lyrics
import org.jellyfin.playback.jellyfin.lyricsFlow

fun initializeLyricsView(
	coverView: AsyncImageView,
	lyricsView: ComposeView,
	playbackManager: PlaybackManager,
) {
	lyricsView.setContent {
		val entry by rememberQueueEntry(playbackManager)
		val lyrics = entry?.run { lyricsFlow.collectAsState(lyrics) }?.value

		// Animate cover view alpha
		val coverViewAlpha by animateFloatAsState(
			label = "CoverViewAlpha",
			targetValue = if (lyrics == null) 1f else 0.2f,
		)
		LaunchedEffect(coverViewAlpha) { coverView.alpha = coverViewAlpha }

		// Display lyrics overlay
		if (lyrics != null) {
			val playState by remember { playbackManager.state.playState }.collectAsState()

			// Using the progress animation causes the layout to recompose, which we need for synced lyrics to work
			// we don't actually use the animation value here
			rememberPlayerProgress(playbackManager)

			LyricsDtoBox(
				lyricDto = lyrics,
				currentTimestamp = playbackManager.state.positionInfo.active,
				duration = playbackManager.state.positionInfo.duration,
				paused = playState != PlayState.PLAYING,
				fontSize = 12.sp,
				color = Color.White,
				modifier = Modifier
					.fillMaxSize()
					.fadingEdges(vertical = 50.dp)
					.padding(horizontal = 15.dp),
			)
		}
	}
}
