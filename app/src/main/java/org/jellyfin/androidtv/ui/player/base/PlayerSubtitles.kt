package org.jellyfin.androidtv.ui.player.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.ui.PlayerSubtitleView
import org.koin.compose.koinInject

@Composable
fun PlayerSubtitles(
	modifier: Modifier = Modifier,
	playbackManager: PlaybackManager = koinInject(),
) = AndroidView(
	factory = { context -> PlayerSubtitleView(context) },
	modifier = modifier,
	update = { view ->
		view.playbackManager = playbackManager
	}
)
