package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes

@Composable
fun SettingsMainScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current

	Column(
		modifier = Modifier
			.verticalScroll(rememberScrollState())
			.padding(6.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		ListSection(
			modifier = Modifier,
			overlineContent = { Text(stringResource(R.string.app_name).uppercase()) },
			headingContent = { Text(stringResource(R.string.settings)) },
			captionContent = { Text(stringResource(R.string.settings_description)) },
		)

		ListButton(
			leadingContent = {
				Icon(
					painterResource(R.drawable.ic_users),
					contentDescription = null
				)
			},
			headingContent = { Text(stringResource(R.string.pref_login)) },
			captionContent = { Text(stringResource(R.string.pref_login_description)) },
			onClick = { router.push(Routes.AUTHENTICATION) }
		)

		ListButton(
			leadingContent = {
				Icon(
					painterResource(R.drawable.ic_adjust),
					contentDescription = null
				)
			},
			headingContent = { Text(stringResource(R.string.pref_customization)) },
			captionContent = { Text(stringResource(R.string.pref_customization_description)) },
			onClick = {
				context.startActivity(ActivityDestinations.customizationPreferences(context))
			}
		)

		ListButton(
			leadingContent = {
				Icon(
					painterResource(R.drawable.ic_next),
					contentDescription = null
				)
			},
			headingContent = { Text(stringResource(R.string.pref_playback)) },
			captionContent = { Text(stringResource(R.string.pref_playback_description)) },
			onClick = {
				context.startActivity(ActivityDestinations.playbackPreferences(context))
			}
		)

		ListButton(
			leadingContent = {
				Icon(
					painterResource(R.drawable.ic_error),
					contentDescription = null
				)
			},
			headingContent = { Text(stringResource(R.string.pref_telemetry_category)) },
			captionContent = { Text(stringResource(R.string.pref_telemetry_description)) },
			onClick = { router.push(Routes.TELEMETRY) }
		)

		ListButton(
			leadingContent = {
				Icon(
					painterResource(R.drawable.ic_flask),
					contentDescription = null
				)
			},
			headingContent = { Text(stringResource(R.string.pref_developer_link)) },
			captionContent = { Text(stringResource(R.string.pref_developer_link_description)) },
			onClick = { router.push(Routes.DEVELOPER) }
		)

		SettingsMainScreenAbout()
	}
}
