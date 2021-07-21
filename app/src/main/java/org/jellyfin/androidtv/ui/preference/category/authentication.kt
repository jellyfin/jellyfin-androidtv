package org.jellyfin.androidtv.ui.preference.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.preference.Preference
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.preference.dsl.OptionsBinder
import org.jellyfin.androidtv.ui.preference.dsl.OptionsItemUserPicker.UserSelection
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.userPicker
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
		authenticationPreferences[userBehaviorPreference] = it.behavior
		authenticationPreferences[userIdPreference] = it.userId?.toString().orEmpty()

		onSet?.invoke(it)
	}

	default {
		UserSelection(UserSelectBehavior.LAST_USER, null)
	}
}
