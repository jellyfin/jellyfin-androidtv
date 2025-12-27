package org.jellyfin.androidtv.ui.settings.screen.screensaver

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
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsScreensaverScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings_title).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_screensaver)) },
			)
		}

		item {
			var screensaverInAppEnabled by rememberPreference(userPreferences, UserPreferences.screensaverInAppEnabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_screensaver_inapp_enabled)) },
				trailingContent = { Checkbox(checked = screensaverInAppEnabled) },
				captionContent = { Text(stringResource(R.string.pref_screensaver_inapp_enabled_description)) },
				onClick = { screensaverInAppEnabled = !screensaverInAppEnabled }
			)
		}

		item {
			var screensaverInAppTimeout by rememberPreference(userPreferences, UserPreferences.screensaverInAppTimeout)
			val caption = getScreensaverTimeoutOptions()
				.firstOrNull { (duration) -> duration.inWholeMilliseconds == screensaverInAppTimeout }
				?.second.orEmpty()

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_screensaver_inapp_timeout)) },
				captionContent = { Text(caption) },
				onClick = { router.push(Routes.CUSTOMIZATION_SCREENSAVER_TIMEOUT) }
			)
		}

		item {
			var screensaverAgeRatingRequired by rememberPreference(userPreferences, UserPreferences.screensaverAgeRatingRequired)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_screensaver_ageratingrequired_title)) },
				trailingContent = { Checkbox(checked = screensaverAgeRatingRequired) },
				captionContent = { Text(stringResource(R.string.pref_screensaver_ageratingrequired_enabled)) },
				onClick = { screensaverAgeRatingRequired = !screensaverAgeRatingRequired }
			)
		}

		item {
			var screensaverAgeRatingMax by rememberPreference(userPreferences, UserPreferences.screensaverAgeRatingMax)
			val caption = getScreensaverAgeRatingOptions()
				.firstOrNull { (ageRating) -> ageRating == screensaverAgeRatingMax }
				?.second.orEmpty()

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_screensaver_ageratingmax)) },
				captionContent = { Text(caption) },
				onClick = { router.push(Routes.CUSTOMIZATION_SCREENSAVER_AGE_RATING) }
			)
		}
	}
}
