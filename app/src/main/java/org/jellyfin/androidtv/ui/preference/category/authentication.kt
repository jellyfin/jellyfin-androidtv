package org.jellyfin.androidtv.ui.preference.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.data.model.LogonCredentials
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.LoginBehavior
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper
import timber.log.Timber
import java.io.IOException

fun OptionsScreen.authenticationCategory(
	userPreferences: UserPreferences
) = category {
	setTitle(R.string.pref_authentication_cat)

	enum<LoginBehavior> {
		setTitle(R.string.pref_login_behavior_title)
		bind {
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
			default { userPreferences.getDefaultValue(UserPreferences.loginBehavior) }
		}
		depends {
			val configuredAutoCredentials = TvApp.getApplication().configuredAutoCredentials

			// Auto login is disabled
			userPreferences[UserPreferences.loginBehavior] != LoginBehavior.AUTO_LOGIN
				// Or configured user is set to current user
				|| configuredAutoCredentials.userDto.id == TvApp.getApplication().currentUser.id
		}
	}

	checkbox {
		setTitle(R.string.pref_prompt_pw)
		bind(userPreferences, UserPreferences.passwordPromptEnabled)
		depends {
			val configuredAutoCredentials = TvApp.getApplication().configuredAutoCredentials

			// Auto login is enabled
			userPreferences[UserPreferences.loginBehavior] == LoginBehavior.AUTO_LOGIN
				// Configured user is set to current user
				&& configuredAutoCredentials.userDto.id == TvApp.getApplication().currentUser.id
				// Configured user contains a password
				&& configuredAutoCredentials.userDto.hasPassword
		}
	}

	checkbox {
		setTitle(R.string.pref_alt_pw_entry)
		setContent(R.string.pref_alt_pw_entry_desc)
		bind(userPreferences, UserPreferences.passwordDPadEnabled)
	}

	checkbox {
		setTitle(R.string.pref_live_tv_mode)
		setContent(R.string.pref_live_tv_mode_desc)
		bind(userPreferences, UserPreferences.liveTvMode)
	}
}
