package org.jellyfin.androidtv.ui.player.base

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.jellyfin.androidtv.ui.player.video.StereoVideoLayout
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.ui.PlayerSubtitleView
import org.jellyfin.sdk.model.api.Video3dFormat
import org.koin.compose.koinInject

@Composable
fun PlayerSubtitles(
	modifier: Modifier = Modifier,
	playbackManager: PlaybackManager = koinInject(),
	video3dFormat: Video3dFormat? = null,
) {
	StereoVideoLayout(video3dFormat, modifier) {
		AndroidView(
			factory = { context -> PlayerSubtitleView(context) },
			modifier = Modifier.fillMaxSize(),
			update = { view ->
				view.playbackManager = playbackManager
			}
		)
	}
}
