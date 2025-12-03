package org.jellyfin.androidtv.ui.settings

import androidx.compose.runtime.Composable
import org.jellyfin.androidtv.ui.settings.screen.SettingsMainScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsTelemetryScreen

object Routes {
	const val MAIN = "/"
	const val TELEMETRY = "/telemetry"
}

val routes = mapOf<String, @Composable () -> Unit>(
	Routes.MAIN to ::SettingsMainScreen,
	Routes.TELEMETRY to ::SettingsTelemetryScreen
)
