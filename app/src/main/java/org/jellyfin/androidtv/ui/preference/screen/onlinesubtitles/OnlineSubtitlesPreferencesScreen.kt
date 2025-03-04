package org.jellyfin.androidtv.ui.preference.screen.onlinesubtitles

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.info
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.screen.onlinesubtitles.opensubtitles.OpenSubtitlesLanguagesScreen
import org.jellyfin.androidtv.ui.preference.screen.onlinesubtitles.opensubtitles.OpenSubtitlesLoginScreen
import org.jellyfin.androidtv.ui.preference.screen.onlinesubtitles.opensubtitles.OpenSubtitlesLogoutScreen
import org.jellyfin.androidtv.ui.preference.screen.onlinesubtitles.subdl.SubdlCustomApiKeyScreen
import org.jellyfin.androidtv.ui.preference.screen.onlinesubtitles.subdl.SubdlLanguagesScreen
import org.jellyfin.preference.store.PreferenceStore
import org.koin.android.ext.android.inject

class OnlineSubtitlesPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()
	private val userSettingPreferences: UserSettingPreferences by inject()

	override val stores: Array<PreferenceStore<*, *>>
		get() = arrayOf(userSettingPreferences)

	override val screen by optionsScreen {
		setTitle(R.string.pref_playback)

		category {
			setTitle(R.string.pref_opensubtitles)

			if (userPreferences[UserPreferences.openSubtitlesToken].isEmpty()) {
				link {
					title = "Login"
					setContent(R.string.pref_opensubtitles)
					icon = R.drawable.ic_user
					withFragment<OpenSubtitlesLoginScreen>()
				}
			} else {
				link {
					title = "Account Details"
					icon = R.drawable.ic_user
					withFragment<OpenSubtitlesLogoutScreen>()
				}
				link {
					title = "Preferred Languages"
					icon = R.drawable.ic_select_subtitle
					content = userPreferences[UserPreferences.openSubtitlesPreferredLanguages]
					withFragment<OpenSubtitlesLanguagesScreen>()
				}
			}

		}



		category {
			title = "SUBDL"

			link {
				title = "Set Custom Api Key"
				icon = R.drawable.ic_settings
				content = userPreferences[UserPreferences.subdlCustomApiKey]
				withFragment<SubdlCustomApiKeyScreen>()
			}
			link {
				title = "Preferred Languages"
				icon = R.drawable.ic_select_subtitle
				content = userPreferences[UserPreferences.subdlPreferredLanguages]
				withFragment<SubdlLanguagesScreen>()
			}
		}

	}

	override val rebuildOnResume: Boolean
		get() = true
}
