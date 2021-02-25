package org.jellyfin.androidtv.ui.startup

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AccountManagerHelper
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.koin.android.ext.android.get
import java.util.*

class EditUserScreen : OptionsFragment() {
	override val screen by lazyOptionsScreen {
		val serverUUID = requireArguments().get(ARG_SERVER_UUID)
		val userUUID = requireArguments().get(ARG_USER_UUID)

		if (serverUUID !is UUID || userUUID !is UUID)
			return@lazyOptionsScreen

		val accountManagerHelper = get<AccountManagerHelper>()
		val authenticationStore = get<AuthenticationStore>()
		val server = authenticationStore.getServer(serverUUID) ?: return@lazyOptionsScreen

		val user = authenticationStore.getUser(serverUUID, userUUID) ?: return@lazyOptionsScreen
		title = context?.getString(R.string.lbl_user_server, user.name, server.name)

		val account = accountManagerHelper.getAccount(userUUID)

		category {
			action {
				setTitle(R.string.lbl_sign_out)
				setContent(R.string.lbl_sign_out_content)

				icon = R.drawable.ic_logout

				onActivate = {
					if (account != null) accountManagerHelper.removeAccount(account)
				}

				// Disable action when access token is not set (already signed out)
				depends {
					accountManagerHelper.getAccount(userUUID)?.accessToken != null
				}
			}

			action {
				setTitle(R.string.lbl_remove)
				setContent(R.string.lbl_remove_user_content)

				icon = R.drawable.ic_delete

				onActivate = {
					authenticationStore.removeUser(serverUUID, userUUID)
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
