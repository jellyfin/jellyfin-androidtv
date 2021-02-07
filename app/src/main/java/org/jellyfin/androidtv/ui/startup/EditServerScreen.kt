package org.jellyfin.androidtv.ui.startup

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.get
import java.text.DateFormat
import java.util.*

class EditServerScreen : OptionsFragment() {

	override val screen by optionsScreen {
		val serverUUID = requireArguments().get(ARG_SERVER_UUID)

		if (serverUUID !is UUID)
			return@optionsScreen

		val authenticationStore = get<AuthenticationStore>()

		val server = authenticationStore.getServer(serverUUID) ?: return@optionsScreen

		title = server.name

		if (server.users.isNotEmpty()) {
			category {
				setTitle(R.string.lbl_users)

				for (user in server.users) {
					link {
						title = user.value.name
						icon = R.drawable.ic_user

						val lastUsedDate = Date(user.value.lastUsed)
						content = context.getString(R.string.lbl_user_last_used,
								DateFormat.getDateInstance(DateFormat.MEDIUM).format(lastUsedDate),
								DateFormat.getTimeInstance(DateFormat.SHORT).format(lastUsedDate))

						withFragment<EditUserScreen>(bundleOf(
								EditUserScreen.ARG_SERVER_UUID to serverUUID,
								EditUserScreen.ARG_USER_UUID to user.key
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
				setAction { _ ->
					authenticationStore.removeServer(serverUUID)

					//Finish the activity because the server list in
					// ManageServersScreen won't update otherwise
					requireActivity().finish()

					return@setAction true
				}
			}
		}

	}

	companion object {
		const val ARG_SERVER_UUID = "server_uuid"
	}
}
