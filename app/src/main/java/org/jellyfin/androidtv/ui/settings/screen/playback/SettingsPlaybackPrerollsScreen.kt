package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackPrerollsScreen() {
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_prerolls)) },
			)
		}

		item {
			var cinemaModeEnabled by rememberPreference(userPreferences, UserPreferences.cinemaModeEnabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_prerolls_enabled)) },
				trailingContent = { Checkbox(checked = cinemaModeEnabled) },
				captionContent = { Text(stringResource(R.string.pref_prerolls_enabled_description)) },
				onClick = { cinemaModeEnabled = !cinemaModeEnabled }
			)
		}
	}
}
