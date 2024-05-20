package org.jellyfin.androidtv.ui.playback

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.ui.composable.LyricsDtoBox
import org.jellyfin.androidtv.ui.composable.modifier.fadingEdges
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.lyricsFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun initializeLyricsView(
	coverView: AsyncImageView,
	lyricsView: ComposeView,
	playbackManager: PlaybackManager,
) {
	lyricsView.setContent {
		val lyrics by remember {
			@OptIn(ExperimentalCoroutinesApi::class)
			playbackManager.queue.entry.flatMapLatest { entry ->
				entry?.lyricsFlow ?: emptyFlow()
			}
		}.collectAsState(null)

		// Animate cover view alpha
		val coverViewAlpha by animateFloatAsState(
			label = "CoverViewAlpha",
			targetValue = if (lyrics == null) 1f else 0.2f,
		)
		LaunchedEffect(coverViewAlpha) { coverView.alpha = coverViewAlpha }

		// Track playback position & duration
		var playbackPosition by remember { mutableStateOf(Duration.ZERO) }
		var playbackDuration by remember { mutableStateOf(Duration.ZERO) }
		val playState by remember { playbackManager.state.playState }.collectAsState()

		LaunchedEffect(lyrics, playState) {
			while (true) {
				val positionInfo = playbackManager.state.positionInfo
				playbackPosition = positionInfo.active
				playbackDuration = positionInfo.duration

				delay(1.seconds)
			}
		}

		// Display lyrics overlay
		if (lyrics != null) {
			LyricsDtoBox(
				lyricDto = lyrics!!,
				currentTimestamp = playbackPosition,
				duration = playbackDuration,
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
