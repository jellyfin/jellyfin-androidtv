package org.jellyfin.androidtv.ui.settings.screen.screensaver

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsScreensaverTimeoutScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var screensaverInAppTimeout by rememberPreference(userPreferences, UserPreferences.screensaverInAppTimeout)
	val options = getScreensaverTimeoutOptions()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_screensaver).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_screensaver_inapp_timeout)) },
			)
		}

		items(options) { (duration, heading) ->
			ListButton(
				headingContent = { Text(heading) },
				trailingContent = { RadioButton(checked = screensaverInAppTimeout == duration.inWholeMilliseconds) },
				onClick = {
					screensaverInAppTimeout = duration.inWholeMilliseconds
					router.back()
				}
			)
		}
	}
}
