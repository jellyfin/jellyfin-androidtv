package org.jellyfin.androidtv.ui.settings

import org.jellyfin.androidtv.ui.navigation.RouteComposable
import org.jellyfin.androidtv.ui.settings.screen.SettingsDeveloperScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsMainScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsTelemetryScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationAutoSignInScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationServerScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationServerUserScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationSortByScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicenseScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicensesScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackInactivityPromptScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackPrerollsScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment.SettingsPlaybackMediaSegmentScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment.SettingsPlaybackMediaSegmentsScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.nextup.SettingsPlaybackNextUpBehaviorScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.nextup.SettingsPlaybackNextUpScreen
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

object Routes {
	const val MAIN = "/"
	const val AUTHENTICATION = "/authentication"
	const val AUTHENTICATION_FROM_LOGIN = "/authentication+login"
	const val AUTHENTICATION_SERVER = "/authentication/server/{serverId}"
	const val AUTHENTICATION_SERVER_USER = "/authentication/server/{serverId}/user/{userId}"
	const val AUTHENTICATION_SORT_BY = "/authentication/sort-by"
	const val AUTHENTICATION_AUTO_SIGN_IN = "/authentication/auto-sign-in"
	const val PLAYBACK = "/playback"
	const val PLAYBACK_NEXT_UP = "/playback/next-up"
	const val PLAYBACK_NEXT_UP_BEHAVIOR = "/playback/next-up/behavior"
	const val PLAYBACK_INACTIVITY_PROMPT = "/playback/inactivity-prompt"
	const val PLAYBACK_PREROLLS = "/playback/prerolls"
	const val PLAYBACK_MEDIA_SEGMENTS = "/playback/media-segments"
	const val PLAYBACK_MEDIA_SEGMENT = "/playback/media-segments/{segmentType}"
	const val TELEMETRY = "/telemetry"
	const val DEVELOPER = "/developer"
	const val LICENSES = "/licenses"
	const val LICENSE = "/license/{artifactId}"
}

val routes = mapOf<String, RouteComposable>(
	Routes.MAIN to {
		SettingsMainScreen()
	},
	Routes.AUTHENTICATION to {
		SettingsAuthenticationScreen(false)
	},
	Routes.AUTHENTICATION_FROM_LOGIN to {
		SettingsAuthenticationScreen(true)
	},
	Routes.AUTHENTICATION_SERVER to { context ->
		SettingsAuthenticationServerScreen(
			serverId = context.parameters["serverId"]?.toUUIDOrNull()!!
		)
	},
	Routes.AUTHENTICATION_SERVER_USER to { context ->
		SettingsAuthenticationServerUserScreen(
			serverId = context.parameters["serverId"]?.toUUIDOrNull()!!,
			userId = context.parameters["userId"]?.toUUIDOrNull()!!
		)
	},
	Routes.AUTHENTICATION_SORT_BY to {
		SettingsAuthenticationSortByScreen()
	},
	Routes.AUTHENTICATION_AUTO_SIGN_IN to {
		SettingsAuthenticationAutoSignInScreen()
	},
	Routes.PLAYBACK to {
		SettingsPlaybackScreen()
	},
	Routes.PLAYBACK_NEXT_UP to {
		SettingsPlaybackNextUpScreen()
	},
	Routes.PLAYBACK_NEXT_UP_BEHAVIOR to {
		SettingsPlaybackNextUpBehaviorScreen()
	},
	Routes.PLAYBACK_INACTIVITY_PROMPT to {
		SettingsPlaybackInactivityPromptScreen()
	},
	Routes.PLAYBACK_PREROLLS to {
		SettingsPlaybackPrerollsScreen()
	},
	Routes.PLAYBACK_MEDIA_SEGMENTS to {
		SettingsPlaybackMediaSegmentsScreen()
	},
	Routes.PLAYBACK_MEDIA_SEGMENT to { context ->
		SettingsPlaybackMediaSegmentScreen(
			segmentType = context.parameters["segmentType"]?.let(MediaSegmentType::fromNameOrNull)!!,
		)
	},
	Routes.TELEMETRY to {
		SettingsTelemetryScreen()
	},
	Routes.DEVELOPER to {
		SettingsDeveloperScreen()
	},
	Routes.LICENSES to {
		SettingsLicensesScreen()
	},
	Routes.LICENSE to { context ->
		SettingsLicenseScreen(
			artifactId = context.parameters["artifactId"]!!
		)
	},
)
