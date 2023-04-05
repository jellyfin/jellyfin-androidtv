package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject

class DeveloperPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

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

			checkbox {
				setTitle(R.string.enable_reactive_homepage)
				setContent(R.string.enable_playback_module_description)
				bind(userPreferences, UserPreferences.homeReactive)
			}

			// Only show in debug mode
			// some strings are hardcoded because these options don't show in beta/release builds
			if (BuildConfig.DEVELOPMENT) {
				checkbox {
					title = "Enable new playback module for video"
					setContent(R.string.enable_playback_module_description)

					bind(userPreferences, UserPreferences.playbackRewriteVideoEnabled)
				}

				checkbox {
					title = "Enable new playback module for audio"
					setContent(R.string.enable_playback_module_description)

					bind(userPreferences, UserPreferences.playbackRewriteAudioEnabled)
				}
			}
		}
	}
}
