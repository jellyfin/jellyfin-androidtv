package org.jellyfin.androidtv.ui.startup

import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
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

				server.users.forEach { user ->
					action {
						title = context.getString(R.string.lbl_remove_user, user.value.name)
						icon = R.drawable.ic_user

						val lastUsedDate = Date(user.value.lastUsed)
						content = context.getString(R.string.lbl_user_last_used,
								DateFormat.getDateInstance(DateFormat.MEDIUM).format(lastUsedDate),
								DateFormat.getTimeInstance(DateFormat.SHORT).format(lastUsedDate))

						setListener { pref ->
							(pref.parent as PreferenceCategory).removePreference(pref)
							authenticationStore.removeUser(serverUUID, user.key)
							return@setListener true
						}
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
				setListener { _ ->
					authenticationStore.removeServer(serverUUID)

					//Pop the back stack manually
					requireActivity().supportFragmentManager.fragments[0].childFragmentManager.popBackStackImmediate()

					return@setListener true
				}
			}
		}

	}

	companion object {
		const val ARG_SERVER_UUID = "server_uuid"
	}
}
