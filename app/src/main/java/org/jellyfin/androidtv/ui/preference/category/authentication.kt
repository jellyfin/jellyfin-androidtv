package org.jellyfin.androidtv.ui.preference.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.LoginBehavior
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.apiclient.interaction.ApiClient

fun OptionsScreen.authenticationCategory(
	userPreferences: UserPreferences,
	apiClient: ApiClient
) = category {
	setTitle(R.string.pref_authentication_cat)

	enum<LoginBehavior> {
		setTitle(R.string.pref_login_behavior_title)
		bind {
			set {
				@Suppress("ControlFlowWithEmptyBody")
				if (it == LoginBehavior.AUTO_LOGIN) {
					// FIXME: Fix autologin preference
				}

				userPreferences[UserPreferences.loginBehavior] = it
			}
			get { userPreferences[UserPreferences.loginBehavior] }
			default { userPreferences.getDefaultValue(UserPreferences.loginBehavior) }
		}
		depends {
			false //FIXME
		}
	}

	checkbox {
		setTitle(R.string.pref_prompt_pw)
		bind(userPreferences, UserPreferences.passwordPromptEnabled)
		depends {
			false //FIXME
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
