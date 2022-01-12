package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.preference.PreferenceStore
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.koin.android.ext.android.inject

class HomePreferencesScreen : OptionsFragment() {
	private val userSettingPreferences: UserSettingPreferences by inject()

	override val stores: Array<PreferenceStore>
		get() = arrayOf(userSettingPreferences)

	override val screen by lazyOptionsScreen {
		setTitle(R.string.home_prefs)

		category {
			setTitle(R.string.home_sections)

			arrayOf(
				UserSettingPreferences.homesection0,
				UserSettingPreferences.homesection1,
				UserSettingPreferences.homesection2,
				UserSettingPreferences.homesection3,
				UserSettingPreferences.homesection4,
				UserSettingPreferences.homesection5,
				UserSettingPreferences.homesection6,
			).forEachIndexed { index, section ->
				enum<HomeSectionType> {
					title = getString(R.string.home_section_i, index + 1)
					bind(userSettingPreferences, section)
				}
			}
		}
	}
}
