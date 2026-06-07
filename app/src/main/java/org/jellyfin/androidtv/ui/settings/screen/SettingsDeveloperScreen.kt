package org.jellyfin.androidtv.ui.settings.screen

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.isTvDevice
import org.koin.compose.koinInject

@Composable
fun SettingsDeveloperScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val systemPreferences = koinInject<SystemPreferences>()
	val context = LocalContext.current
	val isTvDevice = remember(context) { context.isTvDevice() }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_about_title).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_developer_link)) },
			)
		}

		item {
			// Legacy debug flag
			// Not in use by much components anymore
			var debuggingEnabled by rememberPreference(userPreferences, UserPreferences.debuggingEnabled)
			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_enable_debug)) },
				trailingContent = { Checkbox(checked = debuggingEnabled) },
				captionContent = { Text(stringResource(R.string.desc_debug)) },
				onClick = { debuggingEnabled = !debuggingEnabled }
			)
		}

		// UI Mode toggle
		if (!isTvDevice) item {
			var disableUiModeWarning by rememberPreference(systemPreferences, SystemPreferences.disableUiModeWarning)
			ListButton(
				headingContent = { Text(stringResource(R.string.disable_ui_mode_warning)) },
				trailingContent = { Checkbox(checked = disableUiModeWarning) },
				onClick = { disableUiModeWarning = !disableUiModeWarning }
			)
		}

		item {
			// Image cache
			val imageLoader = koinInject<ImageLoader>()
			var imageCacheSize by remember { mutableLongStateOf(imageLoader.diskCache?.size ?: 0L) }
			ListButton(
				headingContent = { Text(stringResource(R.string.clear_image_cache)) },
				captionContent = {
					Text(
						stringResource(
							R.string.clear_image_cache_content,
							Formatter.formatFileSize(context, imageCacheSize)
						)
					)
				},
				onClick = {
					imageLoader.memoryCache?.clear()
					imageLoader.diskCache?.clear()
					imageCacheSize = imageLoader.diskCache?.size ?: 0L
				}
			)
		}
	}
}
