package org.jellyfin.androidtv.ui.settings

import org.jellyfin.androidtv.ui.navigation.RouteComposable
import org.jellyfin.androidtv.ui.settings.screen.SettingsDeveloperScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsMainScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsTelemetryScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicenseScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicensesScreen

object Routes {
	const val MAIN = "/"
	const val TELEMETRY = "/telemetry"
	const val DEVELOPER = "/developer"
	const val LICENSES = "/licenses"
	const val LICENSE = "/license/{artifactId}"
}

val routes = mapOf<String, RouteComposable>(
	Routes.MAIN to { SettingsMainScreen() },
	Routes.TELEMETRY to { SettingsTelemetryScreen() },
	Routes.DEVELOPER to { SettingsDeveloperScreen() },
	Routes.LICENSES to { SettingsLicensesScreen() },
	Routes.LICENSE to { context -> SettingsLicenseScreen(context.parameters["artifactId"]!!) },
)
