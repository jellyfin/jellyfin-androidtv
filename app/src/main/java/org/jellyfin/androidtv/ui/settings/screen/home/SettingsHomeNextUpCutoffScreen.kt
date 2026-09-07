package org.jellyfin.androidtv.ui.settings.screen.home

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import org.jellyfin.androidtv.util.getQuantityString
import org.koin.compose.koinInject

@Composable
fun getNextUpCutoffOptions(): List<Pair<Int, String>> {
	val context = LocalContext.current
	return buildList {
		add(0 to stringResource(R.string.pref_max_days_in_next_up_disabled))
		listOf(7, 14, 30, 60, 90, 180, 365).forEach { days ->
			add(days to context.getQuantityString(R.plurals.days, days))
		}
	}
}

@Composable
fun SettingsHomeNextUpCutoffScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var maxDaysInNextUp by rememberPreference(userPreferences, UserPreferences.maxDaysInNextUp)
	val options = getNextUpCutoffOptions()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.home_prefs).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_max_days_in_next_up)) },
			)
		}

		items(options) { (days, label) ->
			ListButton(
				headingContent = { Text(label) },
				trailingContent = { RadioButton(checked = maxDaysInNextUp == days) },
				onClick = {
					maxDaysInNextUp = days
					router.back()
				},
				modifier = Modifier
					.focusKey("cutoff_$days", initialFocus = maxDaysInNextUp == days)
			)
		}
	}
}
