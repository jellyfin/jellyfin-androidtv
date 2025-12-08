package org.jellyfin.androidtv.ui.settings

import org.jellyfin.androidtv.ui.navigation.RouteComposable
import org.jellyfin.androidtv.ui.settings.screen.SettingsDeveloperScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsMainScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsTelemetryScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationAutoSignInScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationSortByScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicenseScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicensesScreen

object Routes {
	const val MAIN = "/"
	const val AUTHENTICATION = "/authentication"
	const val AUTHENTICATION_SORT_BY = "/authentication/sort-by"
	const val AUTHENTICATION_AUTO_SIGN_IN = "/authentication/auto-sign-in"
	const val TELEMETRY = "/telemetry"
	const val DEVELOPER = "/developer"
	const val LICENSES = "/licenses"
	const val LICENSE = "/license/{artifactId}"
}

val routes = mapOf<String, RouteComposable>(
	Routes.MAIN to { SettingsMainScreen() },
	Routes.AUTHENTICATION to { SettingsAuthenticationScreen() },
	Routes.AUTHENTICATION_SORT_BY to { SettingsAuthenticationSortByScreen() },
	Routes.AUTHENTICATION_AUTO_SIGN_IN to { SettingsAuthenticationAutoSignInScreen() },
	Routes.TELEMETRY to { SettingsTelemetryScreen() },
	Routes.DEVELOPER to { SettingsDeveloperScreen() },
	Routes.LICENSES to { SettingsLicensesScreen() },
	Routes.LICENSE to { context -> SettingsLicenseScreen(context.parameters["artifactId"]!!) },
)
