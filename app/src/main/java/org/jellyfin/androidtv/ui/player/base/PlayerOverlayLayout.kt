package org.jellyfin.androidtv.ui.player.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.ui.composable.modifier.overscan

@Composable
fun PlayerOverlayLayout(
	visible: Boolean,
	modifier: Modifier = Modifier,
	header: (@Composable () -> Unit)? = null,
	controls: (@Composable () -> Unit)? = null,
) = Box(
	modifier = modifier.fillMaxSize()
) {
	if (header != null) {
		AnimatedVisibility(
			visible = visible,
			modifier = Modifier
				.align(Alignment.TopCenter),
			enter = slideInVertically() + fadeIn(),
			exit = slideOutVertically() + fadeOut(),
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight(1f / 3)
					.background(
						brush = Brush.verticalGradient(
							colors = listOf(
								Color.Black.copy(alpha = 0.8f),
								Color.Transparent,
							)
						)
					)
					.overscan()
			) {
				header()
			}
		}
	}

	if (controls != null) {
		AnimatedVisibility(
			visible = visible,
			modifier = Modifier
				.align(Alignment.BottomCenter),
			enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
			exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
		) {
			Box(
				contentAlignment = Alignment.BottomCenter,
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight(1f / 3)
					.background(
						brush = Brush.verticalGradient(
							colors = listOf(
								Color.Transparent,
								Color.Black.copy(alpha = 0.8f),
							)
						)
					)
					.overscan(),
			) {
				controls()
			}
		}
	}
}
