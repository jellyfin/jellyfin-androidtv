package org.jellyfin.androidtv.ui.settings.screen

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.util.isTvDevice
import org.koin.compose.koinInject

@Composable
fun SettingsDeveloperScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val systemPreferences = koinInject<SystemPreferences>()
	val context = LocalContext.current
	val isTvDevice = remember(context) { context.isTvDevice() }
	val isDeveloperBuild = BuildConfig.DEVELOPMENT

	Column(
		modifier = Modifier
			.verticalScroll(rememberScrollState())
			.padding(6.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		ListSection(
			modifier = Modifier,
			overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
			headingContent = { Text(stringResource(R.string.pref_developer_link)) },
			captionContent = { Text(stringResource(R.string.pref_developer_link_description)) },
		)

		// Legacy debug flag
		// Not in use by much components anymore
		var debuggingEnabled by rememberPreference(userPreferences, UserPreferences.debuggingEnabled)
		ListButton(
			headingContent = { Text(stringResource(R.string.lbl_enable_debug)) },
			trailingContent = { RadioButton(checked = debuggingEnabled) },
			captionContent = { Text(stringResource(R.string.desc_debug)) },
			onClick = { debuggingEnabled = !debuggingEnabled }
		)

		// UI Mode toggle
		if (!isTvDevice) {
			var disableUiModeWarning by rememberPreference(systemPreferences, SystemPreferences.disableUiModeWarning)
			ListButton(
				headingContent = { Text(stringResource(R.string.disable_ui_mode_warning)) },
				trailingContent = { RadioButton(checked = disableUiModeWarning) },
				onClick = { disableUiModeWarning = !disableUiModeWarning }
			)
		}

		// Playback rewrite - only show in debug mode
		if (isDeveloperBuild) {
			var playbackRewriteVideoEnabled by rememberPreference(userPreferences, UserPreferences.playbackRewriteVideoEnabled)
			ListButton(
				// String is hardcoded because it's for development only
				headingContent = { Text("Enable new playback module for video") },
				trailingContent = { RadioButton(checked = playbackRewriteVideoEnabled) },
				captionContent = { Text(stringResource(R.string.enable_playback_module_description)) },
				onClick = { playbackRewriteVideoEnabled = !playbackRewriteVideoEnabled }
			)
		}

		// Trick play
		var trickPlayEnabled by rememberPreference(userPreferences, UserPreferences.trickPlayEnabled)
		ListButton(
			headingContent = { Text(stringResource(R.string.preference_enable_trickplay)) },
			trailingContent = { RadioButton(checked = trickPlayEnabled) },
			captionContent = { Text(stringResource(R.string.enable_playback_module_description)) },
			onClick = { trickPlayEnabled = !trickPlayEnabled }
		)

		// FFmpeg audio extension
		var preferExoPlayerFfmpeg by rememberPreference(userPreferences, UserPreferences.preferExoPlayerFfmpeg)
		ListButton(
			headingContent = { Text(stringResource(R.string.prefer_exoplayer_ffmpeg)) },
			trailingContent = { RadioButton(checked = preferExoPlayerFfmpeg) },
			captionContent = { Text(stringResource(R.string.prefer_exoplayer_ffmpeg_content)) },
			onClick = { preferExoPlayerFfmpeg = !preferExoPlayerFfmpeg }
		)

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
