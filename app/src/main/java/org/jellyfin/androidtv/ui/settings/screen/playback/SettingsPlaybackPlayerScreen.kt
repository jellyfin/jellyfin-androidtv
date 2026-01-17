package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ExternalAppRepository
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListMessage
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.componentName
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackPlayerScreen() {
	val externalAppRepository = koinInject<ExternalAppRepository>()
	val context = LocalContext.current
	val router = LocalRouter.current
	val packageManager = context.packageManager

	val externalPlayerApps = remember(context) { externalAppRepository.getExternalPlayerApps(context) }
	val currentExternalPlayer = remember(context) { externalAppRepository.getCurrentExternalPlayerApp(context) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.playback_video_player)) },
			)
		}

		item {
			ListButton(
				leadingContent = {
					Image(
						painter = rememberAsyncImagePainter(R.mipmap.app_icon),
						contentDescription = null,
						modifier = Modifier
							.size(32.dp)
							.clip(LocalShapes.current.small)
					)
				},
				headingContent = { Text(stringResource(R.string.app_name)) },
				trailingContent = { RadioButton(checked = currentExternalPlayer == null) },
				captionContent = { Text(stringResource(R.string.video_player_internal)) },
				onClick = {
					externalAppRepository.setExternalPlayerapp(null)
					router.back()
				}
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.video_player_external)) }) }

		if (externalPlayerApps.isEmpty()) {
			item {
				ListMessage {
					Text(stringResource(R.string.playback_video_player_external_empty))
				}
			}
		}

		items(externalPlayerApps) { app ->
			val icon = remember(app, packageManager) { app.loadIcon(packageManager) }
			val displayName = remember(app, packageManager) { app.loadLabel(packageManager).toString() }

			ListButton(
				leadingContent = {
					Image(
						painter = rememberAsyncImagePainter(icon),
						contentDescription = null,
						modifier = Modifier
							.size(32.dp)
							.clip(LocalShapes.current.small)
					)
				},
				headingContent = { Text(displayName) },
				trailingContent = { RadioButton(checked = currentExternalPlayer?.componentName == app.activityInfo.componentName) },
				captionContent = { Text(stringResource(R.string.video_player_external)) },
				onClick = {
					externalAppRepository.setExternalPlayerapp(app.activityInfo)
					router.back()
				}
			)
		}
	}
}
