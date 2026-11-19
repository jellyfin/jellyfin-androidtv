package org.jellyfin.androidtv.ui.screensaver

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import org.jellyfin.androidtv.integration.dream.composable.DreamHost
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.isMediaSessionKeyEvent
import org.jellyfin.androidtv.util.isOpenItemKeyEvent
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun InAppScreensaver() {
	val interactionTrackerViewModel = koinViewModel<InteractionTrackerViewModel>()
	val visible by interactionTrackerViewModel.visible.collectAsState()
	var content by remember { mutableStateOf<DreamContent?>(null) }
	val navigationRepository = koinInject<NavigationRepository>()

	DialogBase(
		visible = visible,
		onDismissRequest = {
			interactionTrackerViewModel.notifyInteraction(canCancel = true, userInitiated = false)
		},
		scrimColor = Color.Black,
		enterTransition = fadeIn(tween(1_000)),
		exitTransition = fadeOut(tween(1_000)),
		modifier = Modifier
			.fillMaxSize()
			.clickable(
				interactionSource = remember { MutableInteractionSource() },
				indication = null,
			) {
				interactionTrackerViewModel.notifyInteraction(canCancel = true, userInitiated = false)
			}
			.onKeyEvent { event ->
				val showcase = content as? DreamContent.LibraryShowcase
				if (showcase != null && event.nativeKeyEvent.isOpenItemKeyEvent()) {
					if (event.type == KeyEventType.KeyUp) {
						navigationRepository.navigate(Destinations.itemDetails(showcase.item.id))
						interactionTrackerViewModel.notifyInteraction(canCancel = true, userInitiated = true)
					}
					return@onKeyEvent true
				}

				if (!event.nativeKeyEvent.isMediaSessionKeyEvent()) {
					interactionTrackerViewModel.notifyInteraction(
						canCancel = event.type == KeyEventType.KeyUp,
						userInitiated = true,
					)
				}
				false
			}
	) {
		DreamHost(onContentChange = { content = it })
	}
}
