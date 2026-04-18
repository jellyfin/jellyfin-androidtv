package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AVCLevel
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackAVCLevelScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	var userAVCLevel by rememberPreference(userPreferences, UserPreferences.userAVCLevel)
	val manualOptions = AVCLevel.entries.filter { it != AVCLevel.AUTO }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.preference_codecs).uppercase()) },
				headingContent = { Text(stringResource(R.string.user_avc_level)) },
				captionContent = { Text(stringResource(R.string.codec_level_warning)) },
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(AVCLevel.AUTO.nameRes)) },
				trailingContent = { RadioButton(checked = userAVCLevel == AVCLevel.AUTO) },
				onClick = {
					userAVCLevel = AVCLevel.AUTO
					router.back()
				}
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.codec_level_manual)) }) }

		items(manualOptions) { level ->
			ListButton(
				headingContent = { Text(stringResource(level.nameRes)) },
				trailingContent = { RadioButton(checked = userAVCLevel == level) },
				onClick = {
					userAVCLevel = level
					router.back()
				}
			)
		}
	}
}
