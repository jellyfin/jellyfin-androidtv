package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsHomeScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	val userSettingPreferences = koinInject<UserSettingPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.home_prefs)) },
			)
		}

		itemsIndexed(userSettingPreferences.homesections) { index, section ->
			ListButton(
				headingContent = { Text(stringResource(R.string.home_section_i, index + 1)) },
				captionContent = { Text(stringResource(userSettingPreferences[section].nameRes)) },
				onClick = { router.push(Routes.HOME_SECTION, mapOf("index" to index.toString())) },
				modifier = Modifier.focusKey("home_section_$index")
			)
		}

		item {
			ListSection(
				headingContent = { Text(stringResource(R.string.lbl_next_up)) },
			)
		}

		item {
			val maxDaysInNextUp by rememberPreference(userPreferences, UserPreferences.maxDaysInNextUp)
			val options = getNextUpCutoffOptions()
			val selectedCaption = options.firstOrNull { it.first == maxDaysInNextUp }?.second
				?: stringResource(R.string.pref_max_days_in_next_up_disabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_max_days_in_next_up)) },
				captionContent = { Text(selectedCaption) },
				onClick = { router.push(Routes.HOME_NEXT_UP_CUTOFF) },
				modifier = Modifier.focusKey(Routes.HOME_NEXT_UP_CUTOFF)
			)
		}
	}
}
