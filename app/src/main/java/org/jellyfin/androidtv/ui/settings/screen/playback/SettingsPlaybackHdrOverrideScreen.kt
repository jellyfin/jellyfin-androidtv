package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.HdrFormat
import org.jellyfin.androidtv.preference.constant.HdrOverrideMode
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackHdrOverrideScreen(
	format: HdrFormat,
) {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var mode by rememberPreference(userPreferences, format.preference)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.preference_hdr_overrides).uppercase()) },
				headingContent = { Text(stringResource(format.nameRes)) },
				captionContent = { Text(stringResource(format.descriptionRes)) },
			)
		}

		items(HdrOverrideMode.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				trailingContent = { RadioButton(checked = mode == entry) },
				onClick = {
					mode = entry
					router.back()
				}
			)
		}
	}
}
