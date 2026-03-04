package org.jellyfin.androidtv.ui.settings.screen

import android.text.format.Formatter
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.dp
import org.jellyfin.androidtv.util.isTvDevice
import org.jellyfin.design.Tokens
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun SettingsDeveloperScreen() {
	val userPreferences = koinInject<UserPreferences>()
	val systemPreferences = koinInject<SystemPreferences>()
	val context = LocalContext.current
	val isTvDevice = remember(context) { context.isTvDevice() }
	val isDeveloperBuild = BuildConfig.DEVELOPMENT
	var libassEnabled by rememberPreference(
		userPreferences,
		UserPreferences.assDirectPlay
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
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

		// Playback rewrite - only show in debug mode
		if (isDeveloperBuild) item {
			var playbackRewriteVideoEnabled by rememberPreference(userPreferences, UserPreferences.playbackRewriteVideoEnabled)
			ListButton(
				// String is hardcoded because it's for development only
				headingContent = { Text("Enable new playback module for video") },
				trailingContent = { Checkbox(checked = playbackRewriteVideoEnabled) },
				captionContent = { Text(stringResource(R.string.enable_playback_module_description)) },
				onClick = { playbackRewriteVideoEnabled = !playbackRewriteVideoEnabled }
			)
		}

		item {
			// Trick play
			var trickPlayEnabled by rememberPreference(userPreferences, UserPreferences.trickPlayEnabled)
			ListButton(
				headingContent = { Text(stringResource(R.string.preference_enable_trickplay)) },
				trailingContent = { Checkbox(checked = trickPlayEnabled) },
				captionContent = { Text(stringResource(R.string.enable_playback_module_description)) },
				onClick = { trickPlayEnabled = !trickPlayEnabled }
			)
		}

		item {
			// FFmpeg audio extension
			var preferExoPlayerFfmpeg by rememberPreference(userPreferences, UserPreferences.preferExoPlayerFfmpeg)
			ListButton(
				headingContent = { Text(stringResource(R.string.prefer_exoplayer_ffmpeg)) },
				trailingContent = { Checkbox(checked = preferExoPlayerFfmpeg) },
				captionContent = { Text(stringResource(R.string.prefer_exoplayer_ffmpeg_content)) },
				onClick = { preferExoPlayerFfmpeg = !preferExoPlayerFfmpeg }
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

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.enable_libass_subtitles)) },
				trailingContent = { Checkbox(checked = libassEnabled) },
				captionContent = { Text(stringResource(R.string.enable_libass_subtitles_description)) },
				onClick = { libassEnabled = !libassEnabled }
			)
		}

		if(libassEnabled) {
			item {
				var glyphSize by rememberPreference(
					userPreferences,
					UserPreferences.libassGlyphSize
				)

				val interactionSource = remember { MutableInteractionSource() }

				ListControl(
					headingContent = { Text(stringResource(R.string.libass_glyph_size)) },
					interactionSource = interactionSource,
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
					) {
						RangeControl(
							modifier = Modifier
								.height(4.dp)
								.weight(1f),
							interactionSource = interactionSource,
							min = 0f,
							max = 20_000f,
							stepForward = 100f,
							value = glyphSize.toFloat(),
							onValueChange = { glyphSize = it.roundToInt() }
						)

						Spacer(Modifier.width(Tokens.Space.spaceSm))

						Box(
							modifier = Modifier.sizeIn(minWidth = 56.dp),
							contentAlignment = Alignment.CenterEnd
						) {
							Text(glyphSize.toString())
						}
					}
				}
			}

			item {
				var cacheSizeMb by rememberPreference(
					userPreferences,
					UserPreferences.libassCacheSize
				)

				val interactionSource = remember { MutableInteractionSource() }

				ListControl(
					headingContent = { Text(stringResource(R.string.libass_cache_size)) },
					interactionSource = interactionSource,
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
					) {
						RangeControl(
							modifier = Modifier
								.height(4.dp)
								.weight(1f),
							interactionSource = interactionSource,
							min = 0f,
							max = 1024f,
							stepForward = 16f,
							value = cacheSizeMb.toFloat(),
							onValueChange = { cacheSizeMb = it.roundToInt() }
						)

						Spacer(Modifier.width(Tokens.Space.spaceSm))

						Box(
							modifier = Modifier.sizeIn(minWidth = 56.dp),
							contentAlignment = Alignment.CenterEnd
						) {
							Text("$cacheSizeMb")
						}
					}
				}
			}

		}
	}
}
