package org.jellyfin.androidtv.ui.startup.preference

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.ui.preference.dsl.*
import org.jellyfin.androidtv.ui.startup.LoginViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.text.DateFormat
import java.util.*

class EditServerScreen : OptionsFragment() {
	private val loginViewModel: LoginViewModel by sharedViewModel()
	private val authenticationRepository by inject<AuthenticationRepository>()

	override val rebuildOnResume = true

	override val screen: OptionsScreen
		get() = optionsScreen {
			val serverUUID = requireArguments().get(ARG_SERVER_UUID) as? UUID
				?: return@optionsScreen

			val server = loginViewModel.getServer(serverUUID) ?: return@optionsScreen
			val users = authenticationRepository.getUsers(serverUUID) ?: return@optionsScreen

			title = server.name

			if (users.isNotEmpty()) {
				category {
					setTitle(R.string.pref_accounts)

					users.forEach { user ->
						link {
							title = user.name
							icon = R.drawable.ic_user

							val lastUsedDate = Date(user.lastUsed)
							content = context.getString(
								R.string.lbl_user_last_used,
								DateFormat.getDateInstance(DateFormat.MEDIUM).format(lastUsedDate),
								DateFormat.getTimeInstance(DateFormat.SHORT).format(lastUsedDate)
							)

							withFragment<EditUserScreen>(bundleOf(
								EditUserScreen.ARG_SERVER_UUID to user.serverId,
								EditUserScreen.ARG_USER_UUID to user.id
							))
						}
					}
				}
			}

			category {
				setTitle(R.string.lbl_server)

				action {
					setTitle(R.string.lbl_remove_server)
					setContent(R.string.lbl_remove_users)
					icon = R.drawable.ic_delete
					onActivate = {
						loginViewModel.removeServer(serverUUID)

						parentFragmentManager.popBackStack()
					}
				}
			}
		}

	companion object {
		const val ARG_SERVER_UUID = "server_uuid"
	}
}
