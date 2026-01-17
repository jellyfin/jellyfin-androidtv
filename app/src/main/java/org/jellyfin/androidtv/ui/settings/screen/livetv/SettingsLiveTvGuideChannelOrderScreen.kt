package org.jellyfin.androidtv.ui.settings.screen.livetv

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.androidtv.preference.constant.LiveTvChannelOrder
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsLiveTvGuideChannelOrderScreen() {
	val router = LocalRouter.current
	val liveTvPreferences = koinInject<LiveTvPreferences>()
	var channelOrder by rememberPreference(liveTvPreferences, LiveTvPreferences.channelOrder)
	val channelOrderEnum = LiveTvChannelOrder.fromString(channelOrder)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.lbl_sort_by)) },
			)
		}

		items(LiveTvChannelOrder.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				trailingContent = { RadioButton(checked = channelOrderEnum == entry) },
				onClick = {
					channelOrder = entry.stringValue
					router.back()
				}
			)
		}
	}
}
