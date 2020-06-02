package org.jellyfin.androidtv.preferences.ui.category

import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.LogonCredentials
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.enums.LoginBehavior
import org.jellyfin.androidtv.preferences.ui.dsl.bind
import org.jellyfin.androidtv.preferences.ui.dsl.category
import org.jellyfin.androidtv.preferences.ui.dsl.checkboxPreference
import org.jellyfin.androidtv.preferences.ui.dsl.enumPreference
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper
import timber.log.Timber
import java.io.IOException

fun PreferenceScreen.authenticationCategory(
	userPreferences: UserPreferences
) = category(R.string.pref_authentication_cat) {
	enumPreference<LoginBehavior>(R.string.pref_login_behavior_title) {
		set {
			if (it == LoginBehavior.AUTO_LOGIN) {
				try {
					val credentials = LogonCredentials(TvApp.getApplication().apiClient.serverInfo, TvApp.getApplication().currentUser)
					AuthenticationHelper.saveLoginCredentials(credentials, TvApp.CREDENTIALS_PATH)
				} catch (e: IOException) {
					Timber.e(e, "Unable to save logon credentials")
				}
			}

			userPreferences[UserPreferences.loginBehavior] = it
		}
		get { userPreferences[UserPreferences.loginBehavior] }
		visible {
			val configuredAutoCredentials = TvApp.getApplication().configuredAutoCredentials

			// Auto login is disabled
			userPreferences[UserPreferences.loginBehavior] != LoginBehavior.AUTO_LOGIN
				// Or configured user is set to current user
				|| configuredAutoCredentials.userDto.id == TvApp.getApplication().currentUser.id
		}
	}
	checkboxPreference(R.string.pref_prompt_pw) {
		bind(userPreferences, UserPreferences.passwordPromptEnabled)
		visible {
			val configuredAutoCredentials = TvApp.getApplication().configuredAutoCredentials

			// Auto login is enabled
			userPreferences[UserPreferences.loginBehavior] == LoginBehavior.AUTO_LOGIN
				// Configured user is set to current user
				&& configuredAutoCredentials.userDto.id == TvApp.getApplication().currentUser.id
				// Configured user contains a password
				&& configuredAutoCredentials.userDto.hasPassword
		}
	}
	checkboxPreference(R.string.pref_alt_pw_entry, R.string.pref_alt_pw_entry_desc) {
		bind(userPreferences, UserPreferences.passwordDPadEnabled)
	}
	checkboxPreference(R.string.pref_live_tv_mode, R.string.pref_live_tv_mode_desc) {
		bind(userPreferences, UserPreferences.liveTvMode)
	}
}
