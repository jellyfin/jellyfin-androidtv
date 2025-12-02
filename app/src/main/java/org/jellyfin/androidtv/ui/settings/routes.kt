package org.jellyfin.androidtv.ui.settings

import androidx.compose.runtime.Composable
import org.jellyfin.androidtv.ui.settings.screen.SettingsMainScreen

object Routes {
	const val MAIN = "/"
}

val routes = mapOf<String, @Composable () -> Unit>(
	Routes.MAIN to ::SettingsMainScreen,
)
