package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
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
fun SettingsPlaybackMediaSegmentAutoHideDurationScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var autoHideDuration by rememberPreference(userPreferences, UserPreferences.mediaSegmentAutoHideDuration)
	val options = getMediaSegmentAutoHideDurationOptions()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_media_segments).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_skip_button_duration)) },
			)
		}

		items(options.toList()) { (duration, label) ->
			ListButton(
				headingContent = { Text(label) },
				trailingContent = { RadioButton(checked = autoHideDuration == duration) },
				onClick = {
					autoHideDuration = duration
					router.back()
				},
				modifier = Modifier
					.focusKey("media_segment_auto_hide_duration_$duration", initialFocus = autoHideDuration == duration)
			)
		}
	}
}
