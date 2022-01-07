package org.jellyfin.androidtv.ui.preference.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.AuthenticationSortBy
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.preference.Preference
import org.jellyfin.androidtv.preference.PreferenceVal
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.preference.dsl.*
import org.jellyfin.androidtv.ui.preference.dsl.OptionsItemUserPicker.UserSelection
import org.jellyfin.sdk.model.serializer.toUUIDOrNull

fun OptionsScreen.authenticationCategory(
	authenticationRepository: AuthenticationRepository,
	authenticationPreferences: AuthenticationPreferences,
	sessionRepository: SessionRepository,
) = category {
	setTitle(R.string.lbl_settings)

	userPicker(authenticationRepository) {
		setTitle(R.string.auto_sign_in)

		bind {
			from(
				authenticationPreferences,
				AuthenticationPreferences.autoLoginUserBehavior,
				AuthenticationPreferences.autoLoginUserId
			)
		}

		depends {
			!authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]
		}
	}

	userPicker(authenticationRepository) {
		setTitle(R.string.system_user)
		setDialogMessage(R.string.system_user_explanation)

		bind {
			from(
				authenticationPreferences,
				AuthenticationPreferences.systemUserBehavior,
				AuthenticationPreferences.systemUserId
			) {
				// Update current system session
				sessionRepository.restoreDefaultSystemSession()
			}
		}

		depends {
			!authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]
		}
	}

	enum<AuthenticationSortBy> {
		setTitle(R.string.lbl_sort_by)
		bind(authenticationPreferences, AuthenticationPreferences.sortBy)
	}
}


fun OptionsScreen.authenticationAdvancedCategory(
	authenticationPreferences: AuthenticationPreferences,
	sessionRepository: SessionRepository,
) {
	// Disallow changing the "always authenticate" option from the login screen
	// because that would allow a kid to disable the function to access a parent's account
	if (sessionRepository.currentSession.value == null) return

	category {
		setTitle(R.string.advanced_settings)

		checkbox {
			setTitle(R.string.always_authenticate)
			setContent(R.string.always_authenticate_description)
			bind(authenticationPreferences, AuthenticationPreferences.alwaysAuthenticate)
		}
	}
}

/**
 * Helper function to bind two preferences to a user picker.
 */
private fun OptionsBinder.Builder<UserSelection>.from(
	authenticationPreferences: AuthenticationPreferences,
	userBehaviorPreference: Preference<UserSelectBehavior>,
	userIdPreference: Preference<String>,
	onSet: ((UserSelection) -> Unit)? = null,
) {
	get {
		UserSelection(
			authenticationPreferences[userBehaviorPreference],
			authenticationPreferences[userIdPreference].toUUIDOrNull()
		)
	}

	set {
		authenticationPreferences[userBehaviorPreference] = PreferenceVal.EnumT(it.behavior)
		authenticationPreferences[userIdPreference] =
			PreferenceVal.StringT(it.userId?.toString().orEmpty())

		onSet?.invoke(it)
	}

	default {
		UserSelection(UserSelectBehavior.LAST_USER, null)
	}
}
