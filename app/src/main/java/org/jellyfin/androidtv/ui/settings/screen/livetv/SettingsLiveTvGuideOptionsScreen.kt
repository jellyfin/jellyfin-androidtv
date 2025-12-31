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
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsLiveTvGuideOptionsScreen() {
	val router = LocalRouter.current
	val liveTvPreferences = koinInject<LiveTvPreferences>()

	val indicators = listOf(
		rememberPreference(liveTvPreferences, LiveTvPreferences.showHDIndicator) to stringResource(R.string.lbl_hd_programs),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showLiveIndicator) to stringResource(R.string.lbl_live_broadcasts),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showNewIndicator) to stringResource(R.string.lbl_new_episodes),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showPremiereIndicator) to stringResource(R.string.lbl_premieres),
		rememberPreference(liveTvPreferences, LiveTvPreferences.showRepeatIndicator) to stringResource(R.string.lbl_repeat_episodes),
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.lbl_live_tv_guide).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings)) },
			)
		}

		item {
			var channelOrder by rememberPreference(liveTvPreferences, LiveTvPreferences.channelOrder)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_sort_by)) },
				captionContent = { Text(stringResource(LiveTvChannelOrder.fromString(channelOrder).nameRes)) },
				onClick = { router.push(Routes.LIVETV_GUIDE_CHANNEL_ORDER) }
			)
		}

		item {
			var favsAtTop by rememberPreference(liveTvPreferences, LiveTvPreferences.favsAtTop)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_start_favorites)) },
				trailingContent = { Checkbox(checked = favsAtTop) },
				onClick = { favsAtTop = !favsAtTop }
			)
		}

		item {
			var colorCodeGuide by rememberPreference(liveTvPreferences, LiveTvPreferences.colorCodeGuide)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_colored_backgrounds)) },
				trailingContent = { Checkbox(checked = colorCodeGuide) },
				onClick = { colorCodeGuide = !colorCodeGuide }
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.lbl_show_indicators)) }) }

		items(indicators) { (preference, label) ->
			var enabled by preference

			ListButton(
				headingContent = { Text(label) },
				trailingContent = { Checkbox(checked = enabled) },
				onClick = { enabled = !enabled }
			)
		}
	}
}
