package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.HDRSupport
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackHDRSupportScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var hdrSupport by rememberPreference(userPreferences, UserPreferences.hdrSupport)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.preference_codecs).uppercase()) },
				headingContent = { Text(stringResource(R.string.hdr_support)) },
				captionContent = { Text(stringResource(R.string.hdr_support_description)) },
			)
		}

		items(HDRSupport.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				captionContent = { Text(stringResource(entry.descriptionRes)) },
				trailingContent = { RadioButton(checked = hdrSupport == entry) },
				onClick = {
					hdrSupport = entry
					router.back()
				},
				modifier = Modifier.focusKey("hdr_support_${entry.name}")
			)
		}
	}
}
