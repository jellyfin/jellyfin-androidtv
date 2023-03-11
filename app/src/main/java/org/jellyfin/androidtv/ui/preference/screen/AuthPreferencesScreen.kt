package org.jellyfin.androidtv.ui.preference.screen

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.AuthenticationSortBy
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.preference.category.aboutCategory
import org.jellyfin.androidtv.ui.preference.dsl.OptionsBinder
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.OptionsItemUserPicker
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.userPicker
import org.jellyfin.androidtv.ui.startup.preference.EditServerScreen
import org.jellyfin.preference.Preference
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject

class AuthPreferencesScreen : OptionsFragment() {
	private val serverRepository: ServerRepository by inject()
	private val serverUserRepository: ServerUserRepository by inject()
	private val authenticationPreferences: AuthenticationPreferences by inject()
	private val sessionRepository: SessionRepository by inject()

	// Allow the "about" category to be hidden
	private val showAbout by lazy {
		requireArguments().getBoolean(ARG_SHOW_ABOUT, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
				serverRepository.loadStoredServers()
				serverRepository.storedServers.collect { rebuild() }
			}
		}
	}

	override val screen by optionsScreen {
		setTitle(R.string.pref_authentication_cat)

		category {
			userPicker(serverRepository, serverUserRepository) {
				setTitle(R.string.auto_sign_in)

				bind {
					from(
						authenticationPreferences,
						AuthenticationPreferences.autoLoginUserBehavior,
						AuthenticationPreferences.autoLoginServerId,
						AuthenticationPreferences.autoLoginUserId,
					)
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

		val servers = serverRepository.storedServers.value
		if (servers.isNotEmpty()) {
			category {
				setTitle(R.string.lbl_manage_servers)

				for (server in servers) {
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
		serverIdPreference: Preference<String>,
		userIdPreference: Preference<String>,
		onSet: ((OptionsItemUserPicker.UserSelection) -> Unit)? = null,
	) {
		get {
			OptionsItemUserPicker.UserSelection(
				authenticationPreferences[userBehaviorPreference],
				authenticationPreferences[serverIdPreference].toUUIDOrNull(),
				authenticationPreferences[userIdPreference].toUUIDOrNull(),
			)
		}

		set {
			authenticationPreferences[userBehaviorPreference] = it.behavior
			authenticationPreferences[serverIdPreference] = it.serverId?.toString().orEmpty()
			authenticationPreferences[userIdPreference] = it.userId?.toString().orEmpty()

			onSet?.invoke(it)
		}

		default {
			OptionsItemUserPicker.UserSelection(UserSelectBehavior.LAST_USER, null, null)
		}
	}

	companion object {
		const val ARG_SHOW_ABOUT = "show_about"
	}
}
