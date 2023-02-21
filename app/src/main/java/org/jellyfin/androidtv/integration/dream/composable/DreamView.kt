package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jellyfin.androidtv.integration.dream.model.DreamContent

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DreamView(
	content: DreamContent,
	showClock: Boolean,
) = Box(
	modifier = Modifier
		.fillMaxSize()
) {
	AnimatedContent(
		targetState = content,
		transitionSpec = {
			fadeIn(tween(durationMillis = 1_000)) with fadeOut(snap(delayMillis = 1_000))
		},
	) { content ->
		when (content) {
			DreamContent.Logo -> DreamContentLogo()
			is DreamContent.LibraryShowcase -> DreamContentLibraryShowcase(content)
			is DreamContent.NowPlaying -> DreamContentNowPlaying(content)
		}
	}

	// Header overlay
	DreamHeader(
		showLogo = content != DreamContent.Logo,
		showClock = showClock,
	)
}
