package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.map
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.ScreensaverLock
import org.jellyfin.androidtv.ui.player.base.PlayerSubtitles
import org.jellyfin.androidtv.ui.player.base.PlayerSurface
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.koin.compose.koinInject

private const val DefaultVideoAspectRatio = 16f / 9f

@Composable
fun VideoPlayerScreen() {
	val playbackManager = koinInject<PlaybackManager>()

	val backgroundService = koinInject<BackgroundService>()
	LaunchedEffect(backgroundService) {
		backgroundService.clearBackgrounds()
	}

	val playing by remember {
		playbackManager.state.playState.map { it == PlayState.PLAYING }
	}.collectAsState(false)
	ScreensaverLock(
		enabled = playing,
	)

	val videoSize by playbackManager.state.videoSize.collectAsState()
	val aspectRatio = videoSize.aspectRatio.takeIf { !it.isNaN() && it > 0f } ?: DefaultVideoAspectRatio

	Box(
		modifier = Modifier
			.background(Color.Black)
			.fillMaxSize()
	) {
		PlayerSurface(
			playbackManager = playbackManager,
			modifier = Modifier
				.aspectRatio(aspectRatio, videoSize.height < videoSize.width)
				.fillMaxSize()
				.align(Alignment.Center)
		)

		VideoPlayerOverlay(
			playbackManager = playbackManager,
		)

		PlayerSubtitles(
			playbackManager = playbackManager,
			modifier = Modifier
				.aspectRatio(aspectRatio, videoSize.height < videoSize.width)
				.fillMaxSize()
				.align(Alignment.Center)
		)
	}
}
