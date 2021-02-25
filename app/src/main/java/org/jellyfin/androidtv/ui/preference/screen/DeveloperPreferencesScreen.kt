package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.GridDirection
import org.jellyfin.androidtv.ui.preference.dsl.*
import org.koin.android.ext.android.inject

class DeveloperPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()

	override val screen by lazyOptionsScreen {
		setTitle(R.string.pref_developer_link)

		category {
			// Legacy debug flag
			// Not in use by much components anymore
			checkbox {
				setTitle(R.string.lbl_enable_debug)
				setContent(R.string.desc_debug)
				bind(userPreferences, UserPreferences.debuggingEnabled)
			}

			// Changing the grid direction is experimental as the vertical
			// direction doesn't calculate image sizes properly resulting
			// in a weird layout in some cases.
			enum<GridDirection> {
				setTitle(R.string.grid_direction)
				bind(userPreferences, UserPreferences.gridDirection)
			}
		}
	}
}
