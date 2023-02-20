package org.jellyfin.androidtv.ui.startup.preference

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.startup.StartupViewModel
import org.jellyfin.androidtv.util.getValue
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.UUID

class EditUserScreen : OptionsFragment() {
	private val startupViewModel: StartupViewModel by activityViewModel()
	private val authenticationRepository by inject<AuthenticationRepository>()
	private val serverUserRepository: ServerUserRepository by inject()

	override val screen by optionsScreen {
		val serverUUID = requireNotNull(
			requireArguments().getValue<UUID>(ARG_SERVER_UUID)
		) { "Missing server id" }
		val userUUID = requireNotNull(
			requireArguments().getValue<UUID>(ARG_USER_UUID)
		) { "Missing user id" }

		val server = requireNotNull(startupViewModel.getServer(serverUUID)) { "Server not found" }
		val user = requireNotNull(
			serverUserRepository.getStoredServerUsers(server).find { it.id == userUUID }
		) { "User not found" }

		title = context?.getString(R.string.lbl_user_server, user.name, server.name)

		category {
			action {
				setTitle(R.string.lbl_sign_out)
				setContent(R.string.lbl_sign_out_content)

				icon = R.drawable.ic_logout

				onActivate = {
					authenticationRepository.logout(user)
					rebuild()
				}

				// Disable action when access token is not set (already signed out)
				depends {
					user.accessToken != null
				}
			}

			action {
				setTitle(R.string.lbl_remove)
				setContent(R.string.lbl_remove_user_content)

				icon = R.drawable.ic_delete

				onActivate = {
					serverUserRepository.deleteStoredUser(user)
					parentFragmentManager.popBackStack()
				}
			}
		}
	}

	companion object {
		const val ARG_SERVER_UUID = "server_uuid"
		const val ARG_USER_UUID = "user_uuid"
	}
}
