package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback)) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_next_up), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_next_up)) },
				onClick = { router.push(Routes.PLAYBACK_NEXT_UP) }
			)
		}

		item {
			var stillWatchingBehavior by rememberPreference(userPreferences, UserPreferences.stillWatchingBehavior)
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_zzz), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_inactivity_prompt)) },
				captionContent = { Text(stringResource(stillWatchingBehavior.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_INACTIVITY_PROMPT) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_trailer), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_prerolls)) },
				onClick = { router.push(Routes.PLAYBACK_PREROLLS) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_subtitles), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_customization_subtitles)) },
				onClick = { context.startActivity(ActivityDestinations.subtitlePreferences(context)) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_clapperboard), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_media_segments)) },
				onClick = { router.push(Routes.PLAYBACK_MEDIA_SEGMENTS) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_more), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback_advanced)) },
				onClick = { context.startActivity(ActivityDestinations.playbackAdvancedPreferences(context)) }
			)
		}
	}
}
