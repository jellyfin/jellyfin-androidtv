package org.jellyfin.androidtv.ui.settings.screen.playback.nextup

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackNextUpBehaviorScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var nextUpBehavior by rememberPreference(userPreferences, UserPreferences.nextUpBehavior)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_next_up).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_next_up_behavior_title)) },
			)
		}

		items(NextUpBehavior.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				trailingContent = { RadioButton(checked = nextUpBehavior == entry) },
				onClick = {
					nextUpBehavior = entry
					router.back()
				}
			)
		}
	}
}
