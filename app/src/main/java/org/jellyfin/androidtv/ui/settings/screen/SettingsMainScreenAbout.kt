package org.jellyfin.androidtv.ui.settings.screen

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes

@Composable
fun SettingsMainScreenAbout() {
	val router = LocalRouter.current

	ListSection(
		modifier = Modifier,
		headingContent = { Text(stringResource(R.string.pref_about_title)) },
	)

	ListSection(
		leadingContent = {
			Icon(
				painterResource(R.drawable.ic_jellyfin),
				contentDescription = null
			)
		},
		headingContent = { Text("Jellyfin app version") },
		captionContent = { Text("jellyfin-androidtv ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}") },
	)

	ListSection(
		leadingContent = {
			Icon(
				painterResource(R.drawable.ic_tv),
				contentDescription = null
			)
		},
		headingContent = { Text(stringResource(R.string.pref_device_model)) },
		captionContent = { Text("${Build.MANUFACTURER} ${Build.MODEL}") },
	)

	ListButton(
		leadingContent = {
			Icon(
				painterResource(R.drawable.ic_guide),
				contentDescription = null
			)
		},
		headingContent = { Text(stringResource(R.string.licenses_link)) },
		captionContent = { Text(stringResource(R.string.licenses_link_description)) },
		onClick = { router.push(Routes.LICENSES) }
	)
}
