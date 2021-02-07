package org.jellyfin.androidtv.ui.startup

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AccountManagerHelper
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.get
import java.util.*

class EditUserScreen : OptionsFragment() {

	override val screen by optionsScreen {
		val serverUUID = requireArguments().get(ARG_SERVER_UUID)
		val userUUID = requireArguments().get(ARG_USER_UUID)

		if (serverUUID !is UUID || userUUID !is UUID)
			return@optionsScreen

		val accountManagerHelper = get<AccountManagerHelper>()
		val authenticationStore = get<AuthenticationStore>()
		val server = authenticationStore.getServer(serverUUID) ?: return@optionsScreen

		val user = authenticationStore.getUser(serverUUID, userUUID) ?: return@optionsScreen
		title = context?.getString(R.string.lbl_user_server, user.name, server.name)

		val account = accountManagerHelper.getAccount(userUUID)

		category {
			action {
				setTitle(R.string.lbl_sign_out)
				setContent(R.string.lbl_sign_out_content)

				icon = R.drawable.ic_logout

				when {
					account?.accessToken != null -> {
						setAction { pref ->
							accountManagerHelper.removeAccount(account)

							pref.isEnabled = false

							return@setAction true
						}
					}
					else -> enabled = false
				}
			}

			action {
				setTitle(R.string.lbl_remove)
				setContent(R.string.lbl_remove_user_content)

				icon = R.drawable.ic_delete

				setAction { _ ->
					authenticationStore.removeUser(serverUUID, userUUID)

					//Pop the back stack manually
					//Do it twice because the EditServerScreen doesn't update to remove the listed user we just deleted
					val fragment = requireActivity().supportFragmentManager.findFragmentByTag(PreferencesActivity.FRAGMENT_TAG)
					if (fragment != null) {
						fragment.childFragmentManager.popBackStackImmediate()
						fragment.childFragmentManager.popBackStackImmediate()
					} else {
						requireActivity().finish()
					}

					return@setAction true
				}
			}
		}
	}

	companion object {
		const val ARG_SERVER_UUID = "server_uuid"
		const val ARG_USER_UUID = "user_uuid"
	}
}
