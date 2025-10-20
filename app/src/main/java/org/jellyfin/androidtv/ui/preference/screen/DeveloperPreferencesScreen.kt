package org.jellyfin.androidtv.ui.preference.screen

import android.text.format.Formatter
import coil3.ImageLoader
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.util.isTvDevice
import org.koin.android.ext.android.inject

class DeveloperPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()
	private val systemPreferences: SystemPreferences by inject()
	private val imageLoader: ImageLoader by inject()

	override val screen by optionsScreen {
		setTitle(R.string.pref_developer_link)

		category {
			// Legacy debug flag
			// Not in use by much components anymore
			checkbox {
				setTitle(R.string.lbl_enable_debug)
				setContent(R.string.desc_debug)
				bind(userPreferences, UserPreferences.debuggingEnabled)
			}

			// UI Mode toggle
			if (!context.isTvDevice()) {
				checkbox {
					setTitle(R.string.disable_ui_mode_warning)
					bind(systemPreferences, SystemPreferences.disableUiModeWarning)
				}
			}

			// Only show in debug mode
			// some strings are hardcoded because these options don't show in beta/release builds
			if (BuildConfig.DEVELOPMENT) {
				checkbox {
					title = "Enable new playback module for video"
					setContent(R.string.enable_playback_module_description)

					bind(userPreferences, UserPreferences.playbackRewriteVideoEnabled)
				}
			}

			checkbox {
				setTitle(R.string.preference_enable_trickplay)
				setContent(R.string.enable_playback_module_description)

				bind(userPreferences, UserPreferences.trickPlayEnabled)
			}

			checkbox {
				setTitle(R.string.prefer_exoplayer_ffmpeg)
				setContent(R.string.prefer_exoplayer_ffmpeg_content)

				bind(userPreferences, UserPreferences.preferExoPlayerFfmpeg)
			}

			action {
				setTitle(R.string.clear_image_cache)
				content = getString(R.string.clear_image_cache_content, Formatter.formatFileSize(context, imageLoader.diskCache?.size ?: 0))
				onActivate = {
					imageLoader.memoryCache?.clear()
					imageLoader.diskCache?.clear()
					rebuild()
				}
			}
		}
	}
}
