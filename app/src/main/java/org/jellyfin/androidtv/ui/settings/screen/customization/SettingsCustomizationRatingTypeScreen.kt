package org.jellyfin.androidtv.ui.settings.screen.customization

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsCustomizationRatingTypeScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var defaultRatingType by rememberPreference(userPreferences, UserPreferences.defaultRatingType)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_default_rating)) },
			)
		}

		items(RatingType.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				trailingContent = { RadioButton(checked = defaultRatingType == entry) },
				onClick = {
					defaultRatingType = entry
					router.back()
				}
			)
		}
	}
}
