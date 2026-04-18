package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackCodecScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.preference_codecs)) },
				captionContent = { Text(stringResource(R.string.preference_codecs_summary)) },
			)
		}

		item {
			var userAVCLevel by rememberPreference(userPreferences, UserPreferences.userAVCLevel)

			ListButton(
				headingContent = { Text(stringResource(R.string.user_avc_level)) },
				captionContent = { Text(stringResource(userAVCLevel.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_AVC_LEVEL) }
			)
		}

		item {
			var userHEVCLevel by rememberPreference(userPreferences, UserPreferences.userHEVCLevel)

			ListButton(
				headingContent = { Text(stringResource(R.string.user_hevc_level)) },
				captionContent = { Text(stringResource(userHEVCLevel.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_HEVC_LEVEL) }
			)
		}
	}
}
