package org.jellyfin.androidtv.ui.screensaver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.integration.dream.composable.DreamHost
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun InAppScreensaver() {
	val interactionTrackerViewModel = koinViewModel<InteractionTrackerViewModel>()
	val visible by interactionTrackerViewModel.visible.collectAsState()

	AnimatedVisibility(
		visible = visible,
		enter = fadeIn(tween(1_000)),
		exit = fadeOut(tween(1_000)),
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black)
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
				) {
					interactionTrackerViewModel.notifyInteraction(canCancel = true, userInitiated = false)
				}
		) {
			DreamHost()
		}
	}
}
