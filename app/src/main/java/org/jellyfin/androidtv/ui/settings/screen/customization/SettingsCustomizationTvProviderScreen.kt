package org.jellyfin.androidtv.ui.settings.screen.customization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsCustomizationTvProviderScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val workManager = koinInject<WorkManager>()

	// Update the channels with the new settings
	val refreshChannels = { workManager.enqueue(OneTimeWorkRequestBuilder<LeanbackChannelWorker>().build()) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_tv_provider)) },
			)
		}

		item {
			var tvProviderEnabled by rememberPreference(userPreferences, UserPreferences.tvProviderEnabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.tv_provider_enabled)) },
				captionContent = { Text(stringResource(R.string.tv_provider_enabled_description)) },
				trailingContent = { Checkbox(checked = tvProviderEnabled) },
				onClick = {
					tvProviderEnabled = !tvProviderEnabled
					refreshChannels()
				},
				modifier = Modifier.focusKey("leanback_channels_enabled")
			)
		}

		item {
			var groupByFolder by rememberPreference(userPreferences, UserPreferences.tvProviderGroupByFolder)

			ListButton(
				headingContent = { Text(stringResource(R.string.tv_provider_group_by_folder)) },
				captionContent = { Text(stringResource(R.string.tv_provider_group_by_folder_description)) },
				trailingContent = { Checkbox(checked = groupByFolder) },
				onClick = {
					groupByFolder = !groupByFolder
					refreshChannels()
				},
				modifier = Modifier.focusKey("leanback_channels_group_by_folder")
			)
		}

		item {
			var groupBySeries by rememberPreference(userPreferences, UserPreferences.tvProviderGroupBySeries)

			ListButton(
				headingContent = { Text(stringResource(R.string.tv_provider_group_by_series)) },
				captionContent = { Text(stringResource(R.string.tv_provider_group_by_series_description)) },
				trailingContent = { Checkbox(checked = groupBySeries) },
				onClick = {
					groupBySeries = !groupBySeries
					refreshChannels()
				},
				modifier = Modifier.focusKey("leanback_channels_group_by_series")
			)
		}
	}
}
