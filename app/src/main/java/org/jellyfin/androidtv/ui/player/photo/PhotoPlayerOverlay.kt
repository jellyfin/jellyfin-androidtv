package org.jellyfin.androidtv.ui.player.photo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import org.jellyfin.androidtv.ui.player.base.PlayerOverlayLayout
import org.jellyfin.androidtv.ui.player.base.rememberPlayerOverlayVisibility
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.androidx.compose.koinViewModel

@Composable
fun PhotoPlayerOverlay(
	item: BaseItemDto?,
) {
	val viewModel = koinViewModel<PhotoPlayerViewModel>()
	val visibilityState = rememberPlayerOverlayVisibility()

	PlayerOverlayLayout(
		visibilityState = visibilityState,
		header = {
			PhotoPlayerHeader(
				item = item,
			)
		},
		controls = {
			PhotoPlayerControls()
		},
		modifier = Modifier
			.onKeyEvent { event ->
				if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

				when (event.key) {
					Key.MediaPlayPause,
					Key.MediaPlay,
					Key.MediaPause -> {
						viewModel.togglePresentation()
						true
					}

					Key.MediaStepBackward,
					Key.MediaSkipBackward,
					Key.MediaPrevious -> {
						viewModel.showPrevious()
						true
					}

					Key.MediaFastForward,
					Key.MediaSkipForward,
					Key.MediaNext -> {
						viewModel.showNext()
						true
					}

					else -> false
				}
			}
	)
}
