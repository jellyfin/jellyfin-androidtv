package org.jellyfin.androidtv.ui.settings

import org.jellyfin.androidtv.ui.navigation.RouteComposable
import org.jellyfin.androidtv.ui.settings.screen.SettingsDeveloperScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsMainScreen
import org.jellyfin.androidtv.ui.settings.screen.SettingsTelemetryScreen
import org.jellyfin.androidtv.ui.settings.screen.about.SettingsAboutScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationAutoSignInScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationServerScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationServerUserScreen
import org.jellyfin.androidtv.ui.settings.screen.authentication.SettingsAuthenticationSortByScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.SettingsCustomizationClockScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.SettingsCustomizationScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.SettingsCustomizationThemeScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.SettingsCustomizationWatchedIndicatorScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.SettingsSubtitleTextStrokeColorScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.SettingsSubtitlesBackgroundColorScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.SettingsSubtitlesScreen
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.SettingsSubtitlesTextColorScreen
import org.jellyfin.androidtv.ui.settings.screen.home.SettingsHomeScreen
import org.jellyfin.androidtv.ui.settings.screen.home.SettingsHomeSectionScreen
import org.jellyfin.androidtv.ui.settings.screen.library.SettingsLibrariesDisplayGridScreen
import org.jellyfin.androidtv.ui.settings.screen.library.SettingsLibrariesDisplayImageSizeScreen
import org.jellyfin.androidtv.ui.settings.screen.library.SettingsLibrariesDisplayImageTypeScreen
import org.jellyfin.androidtv.ui.settings.screen.library.SettingsLibrariesDisplayScreen
import org.jellyfin.androidtv.ui.settings.screen.library.SettingsLibrariesScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicenseScreen
import org.jellyfin.androidtv.ui.settings.screen.license.SettingsLicensesScreen
import org.jellyfin.androidtv.ui.settings.screen.livetv.SettingsLiveTvGuideChannelOrderScreen
import org.jellyfin.androidtv.ui.settings.screen.livetv.SettingsLiveTvGuideFiltersScreen
import org.jellyfin.androidtv.ui.settings.screen.livetv.SettingsLiveTvGuideOptionsScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackAdvancedScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackAudioBehaviorScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackInactivityPromptScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackMaxBitrateScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackPlayerScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackPrerollsScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackRefreshRateSwitchingBehaviorScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackResumeSubtractDurationScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.SettingsPlaybackZoomModeScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment.SettingsPlaybackMediaSegmentScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment.SettingsPlaybackMediaSegmentsScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.nextup.SettingsPlaybackNextUpBehaviorScreen
import org.jellyfin.androidtv.ui.settings.screen.playback.nextup.SettingsPlaybackNextUpScreen
import org.jellyfin.androidtv.ui.settings.screen.screensaver.SettingsScreensaverAgeRatingScreen
import org.jellyfin.androidtv.ui.settings.screen.screensaver.SettingsScreensaverScreen
import org.jellyfin.androidtv.ui.settings.screen.screensaver.SettingsScreensaverTimeoutScreen
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
	const val CUSTOMIZATION = "/customization"
	const val CUSTOMIZATION_THEME = "/customization/theme"
	const val CUSTOMIZATION_CLOCK = "/customization/clock"
	const val CUSTOMIZATION_WATCHED_INDICATOR = "/customization/watch-indicators"
	const val CUSTOMIZATION_SCREENSAVER = "/customization/screensaver"
	const val CUSTOMIZATION_SCREENSAVER_TIMEOUT = "/customization/screensaver/timeout"
	const val CUSTOMIZATION_SCREENSAVER_AGE_RATING = "/customization/screensaver/age-rating"
	const val CUSTOMIZATION_SUBTITLES = "/customization/subtitles"
	const val CUSTOMIZATION_SUBTITLES_TEXT_COLOR = "/customization/subtitles/text-color"
	const val CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR = "/customization/subtitles/background-color"
	const val CUSTOMIZATION_SUBTITLES_EDGE_COLOR = "/customization/subtitles/edge-color"
	const val LIBRARIES = "/libraries"
	const val LIBRARIES_DISPLAY = "/libraries/display/{itemId}/{displayPreferencesId}"
	const val LIBRARIES_DISPLAY_IMAGE_SIZE = "/libraries/display/{itemId}/{displayPreferencesId}/image-size"
	const val LIBRARIES_DISPLAY_IMAGE_TYPE = "/libraries/display/{itemId}/{displayPreferencesId}/image-type"
	const val LIBRARIES_DISPLAY_GRID = "/libraries/display/{itemId}/{displayPreferencesId}/grid"
	const val HOME = "/home"
	const val HOME_SECTION = "/home/section/{index}"
	const val LIVETV_GUIDE_FILTERS = "/livetv/guide/filters"
	const val LIVETV_GUIDE_OPTIONS = "/livetv/guide/options"
	const val LIVETV_GUIDE_CHANNEL_ORDER = "/livetv/guide/channel-order"
	const val PLAYBACK = "/playback"
	const val PLAYBACK_PLAYER = "/playback/player"
	const val PLAYBACK_NEXT_UP = "/playback/next-up"
	const val PLAYBACK_NEXT_UP_BEHAVIOR = "/playback/next-up/behavior"
	const val PLAYBACK_INACTIVITY_PROMPT = "/playback/inactivity-prompt"
	const val PLAYBACK_PREROLLS = "/playback/prerolls"
	const val PLAYBACK_MEDIA_SEGMENTS = "/playback/media-segments"
	const val PLAYBACK_MEDIA_SEGMENT = "/playback/media-segments/{segmentType}"
	const val PLAYBACK_ADVANCED = "/playback/advanced"
	const val PLAYBACK_RESUME_SUBTRACT_DURATION = "/playback/resume-subtract-duration"
	const val PLAYBACK_MAX_BITRATE = "/playback/max-bitrate"
	const val PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR = "/playback/refresh-rate-switching-behavior"
	const val PLAYBACK_ZOOM_MODE = "/playback/zoom-mode"
	const val PLAYBACK_AUDIO_BEHAVIOR = "/playback/audio-behavior"
	const val TELEMETRY = "/telemetry"
	const val DEVELOPER = "/developer"
	const val ABOUT = "/about"
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
	Routes.CUSTOMIZATION to {
		SettingsCustomizationScreen()
	},
	Routes.CUSTOMIZATION_THEME to {
		SettingsCustomizationThemeScreen()
	},
	Routes.CUSTOMIZATION_CLOCK to {
		SettingsCustomizationClockScreen()
	},
	Routes.CUSTOMIZATION_WATCHED_INDICATOR to {
		SettingsCustomizationWatchedIndicatorScreen()
	},
	Routes.CUSTOMIZATION_SCREENSAVER to {
		SettingsScreensaverScreen()
	},
	Routes.CUSTOMIZATION_SCREENSAVER_TIMEOUT to {
		SettingsScreensaverTimeoutScreen()
	},
	Routes.CUSTOMIZATION_SCREENSAVER_AGE_RATING to {
		SettingsScreensaverAgeRatingScreen()
	},
	Routes.CUSTOMIZATION_SUBTITLES to {
		SettingsSubtitlesScreen()
	},
	Routes.CUSTOMIZATION_SUBTITLES_TEXT_COLOR to {
		SettingsSubtitlesTextColorScreen()
	},
	Routes.CUSTOMIZATION_SUBTITLES_BACKGROUND_COLOR to {
		SettingsSubtitlesBackgroundColorScreen()
	},
	Routes.CUSTOMIZATION_SUBTITLES_EDGE_COLOR to {
		SettingsSubtitleTextStrokeColorScreen()
	},
	Routes.LIBRARIES to {
		SettingsLibrariesScreen()
	},
	Routes.LIBRARIES_DISPLAY to { context ->
		SettingsLibrariesDisplayScreen(context.parameters["itemId"]?.toUUIDOrNull()!!, context.parameters["displayPreferencesId"]!!)
	},
	Routes.LIBRARIES_DISPLAY_IMAGE_SIZE to { context ->
		SettingsLibrariesDisplayImageSizeScreen(
			context.parameters["itemId"]?.toUUIDOrNull()!!,
			context.parameters["displayPreferencesId"]!!
		)
	},
	Routes.LIBRARIES_DISPLAY_IMAGE_TYPE to { context ->
		SettingsLibrariesDisplayImageTypeScreen(
			context.parameters["itemId"]?.toUUIDOrNull()!!,
			context.parameters["displayPreferencesId"]!!
		)
	},
	Routes.LIBRARIES_DISPLAY_GRID to { context ->
		SettingsLibrariesDisplayGridScreen(context.parameters["itemId"]?.toUUIDOrNull()!!, context.parameters["displayPreferencesId"]!!)
	},
	Routes.HOME to {
		SettingsHomeScreen()
	},
	Routes.HOME_SECTION to { context ->
		SettingsHomeSectionScreen(context.parameters["index"]?.toInt()!!)
	},
	Routes.LIVETV_GUIDE_FILTERS to {
		SettingsLiveTvGuideFiltersScreen()
	},
	Routes.LIVETV_GUIDE_OPTIONS to {
		SettingsLiveTvGuideOptionsScreen()
	},
	Routes.LIVETV_GUIDE_CHANNEL_ORDER to {
		SettingsLiveTvGuideChannelOrderScreen()
	},
	Routes.PLAYBACK to {
		SettingsPlaybackScreen()
	},
	Routes.PLAYBACK_PLAYER to {
		SettingsPlaybackPlayerScreen()
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
	Routes.PLAYBACK_ADVANCED to {
		SettingsPlaybackAdvancedScreen()
	},
	Routes.PLAYBACK_RESUME_SUBTRACT_DURATION to {
		SettingsPlaybackResumeSubtractDurationScreen()
	},
	Routes.PLAYBACK_MAX_BITRATE to {
		SettingsPlaybackMaxBitrateScreen()
	},
	Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR to {
		SettingsPlaybackRefreshRateSwitchingBehaviorScreen()
	},
	Routes.PLAYBACK_ZOOM_MODE to {
		SettingsPlaybackZoomModeScreen()
	},
	Routes.PLAYBACK_AUDIO_BEHAVIOR to {
		SettingsPlaybackAudioBehaviorScreen()
	},
	Routes.TELEMETRY to {
		SettingsTelemetryScreen()
	},
	Routes.DEVELOPER to {
		SettingsDeveloperScreen()
	},
	Routes.ABOUT to { context ->
		SettingsAboutScreen(context.parameters["fromLogin"] == "true")
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
