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

			// Only show in debug mode
			if (BuildConfig.DEVELOPMENT) {
				checkbox {
					setTitle(R.string.enable_playback_module_title)
					setContent(R.string.enable_playback_module_description)

					bind(userPreferences, UserPreferences.playbackRewriteEnabled)
				}

				checkbox {
					title = getString(R.string.enable_picture_viewer_title)
					setContent(R.string.enable_playback_module_description)

					bind(userPreferences, UserPreferences.pictureViewerRewriteEnabled)
				}
			}
		}
	}
}
