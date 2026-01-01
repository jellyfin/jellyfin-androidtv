package org.jellyfin.androidtv.ui.browsing

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsRouterContent
import org.jellyfin.androidtv.ui.settings.routes
import java.util.UUID

fun BrowseGridFragment.createSettingsVisibility() = MutableStateFlow(false)

fun BrowseGridFragment.addSettings(
	view: ComposeView,
	itemId: UUID,
	displayPreferencesId: String,
	visible: MutableStateFlow<Boolean>,
) {
	view.setContent {
		val isVisible by visible.collectAsState(false)

		JellyfinTheme {
			ProvideRouter(routes, Routes.LIBRARIES_DISPLAY, mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)) {
				SettingsDialog(
					visible = isVisible,
					onDismissRequest = {
						visible.value = false
						onResume()
					}
				) {
					SettingsRouterContent()
				}
			}
		}
	}
}
