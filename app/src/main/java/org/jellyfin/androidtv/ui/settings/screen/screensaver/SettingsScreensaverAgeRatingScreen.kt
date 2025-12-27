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
fun SettingsScreensaverAgeRatingScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var screensaverAgeRatingMax by rememberPreference(userPreferences, UserPreferences.screensaverAgeRatingMax)
	val options = getScreensaverAgeRatingOptions()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_screensaver).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_screensaver_ageratingmax)) },
			)
		}

		items(options) { (ageRating, heading) ->
			ListButton(
				headingContent = { Text(heading) },
				trailingContent = { RadioButton(checked = screensaverAgeRatingMax == ageRating) },
				onClick = {
					screensaverAgeRatingMax = ageRating
					router.back()
				}
			)
		}
	}
}
