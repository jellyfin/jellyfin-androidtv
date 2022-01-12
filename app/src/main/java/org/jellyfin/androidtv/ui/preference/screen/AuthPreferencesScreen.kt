package org.jellyfin.androidtv.ui.preference.screen

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.AuthenticationSortBy
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.preference.Preference
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.preference.category.aboutCategory
import org.jellyfin.androidtv.ui.preference.dsl.*
import org.jellyfin.androidtv.ui.startup.preference.EditServerScreen
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject

class AuthPreferencesScreen : OptionsFragment() {
	private val authenticationRepository: AuthenticationRepository by inject()
	private val authenticationPreferences: AuthenticationPreferences by inject()
	private val sessionRepository: SessionRepository by inject()

	// Allow the "about" category to be hidden
	private val showAbout by lazy {
		requireArguments().getBoolean(ARG_SHOW_ABOUT, false)
	}

	override val rebuildOnResume = true

	override val screen get() = optionsScreen {
		setTitle(R.string.pref_authentication_cat)

		category {
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
				setTitle(R.string.sort_accounts_by)
				bind(authenticationPreferences, AuthenticationPreferences.sortBy)
			}
		}

		category {
			setTitle(R.string.lbl_manage_servers)

			authenticationRepository.getServers().forEach { server ->
				link {
					title = server.name
					icon = R.drawable.ic_house
					content = server.address
					withFragment<EditServerScreen>(bundleOf(
						EditServerScreen.ARG_SERVER_UUID to server.id
					))
				}
			}
		}

		// Disallow changing the "always authenticate" option from the login screen
		// because that would allow a kid to disable the function to access a parent's account
		if (sessionRepository.currentSession.value != null) {
			category {
				setTitle(R.string.advanced_settings)

				checkbox {
					setTitle(R.string.always_authenticate)
					setContent(R.string.always_authenticate_description)
					bind(authenticationPreferences, AuthenticationPreferences.alwaysAuthenticate)
				}
			}
		}

		if (showAbout) aboutCategory()
	}

	/**
	 * Helper function to bind two preferences to a user picker.
	 */
	private fun OptionsBinder.Builder<OptionsItemUserPicker.UserSelection>.from(
		authenticationPreferences: AuthenticationPreferences,
		userBehaviorPreference: Preference<UserSelectBehavior>,
		userIdPreference: Preference<String>,
		onSet: ((OptionsItemUserPicker.UserSelection) -> Unit)? = null,
	) {
		get {
			OptionsItemUserPicker.UserSelection(
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
			OptionsItemUserPicker.UserSelection(UserSelectBehavior.LAST_USER, null)
		}
	}

	companion object {
		const val ARG_SHOW_ABOUT = "show_about"
	}
}
