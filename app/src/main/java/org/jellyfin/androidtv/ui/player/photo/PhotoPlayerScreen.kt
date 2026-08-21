package org.jellyfin.androidtv.ui.player.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.ScreensaverLock
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsRouterContent
import org.jellyfin.androidtv.ui.settings.routes
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun PhotoPlayerScreen() {
	val viewModel = koinViewModel<PhotoPlayerViewModel>()
	val item by viewModel.currentItem.collectAsState()
	val presentationActive by viewModel.presentationActive.collectAsState()
	val settingsVisible by viewModel.settingsVisible.collectAsState()

	val backgroundService = koinInject<BackgroundService>()
	LaunchedEffect(backgroundService) {
		backgroundService.clearBackgrounds()
	}

	ScreensaverLock(
		enabled = presentationActive,
	)

	Box(
		modifier = Modifier
			.background(Color.Black)
			.fillMaxSize()
	) {
		PhotoPlayerContent(
			item = item,
		)

		PhotoPlayerOverlay(
			item = item,
		)

		ProvideRouter(
			routes,
			Routes.PLAYBACK_PHOTO_PLAYER,
		) {
			SettingsDialog(
				visible = settingsVisible,
				onDismissRequest = {
					viewModel.setSettingsVisible(false);
				}
			) {
				SettingsRouterContent()
			}
		}
	}
}
