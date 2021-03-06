package org.jellyfin.androidtv.ui.startup.preference

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.ui.preference.dsl.*
import org.koin.android.ext.android.get
import java.text.DateFormat
import java.util.*

class EditServerScreen : OptionsFragment() {
	init {
		rebuildOnResume = true
	}

	override val screen: OptionsScreen
		get() = optionsScreen {
			val serverUUID = requireArguments().get(ARG_SERVER_UUID) as? UUID
				?: return@optionsScreen
			val authenticationStore = get<AuthenticationStore>()
			val server = authenticationStore.getServer(serverUUID) ?: return@optionsScreen

			title = server.name

			if (server.users.isNotEmpty()) {
				category {
					setTitle(R.string.lbl_users)

					server.users.forEach { user ->
						link {
							title = user.value.name
							icon = R.drawable.ic_user

							val lastUsedDate = Date(user.value.lastUsed)
							content = context.getString(
								R.string.lbl_user_last_used,
								DateFormat.getDateInstance(DateFormat.MEDIUM).format(lastUsedDate),
								DateFormat.getTimeInstance(DateFormat.SHORT).format(lastUsedDate)
							)

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
					onActivate = {
						authenticationStore.removeServer(serverUUID)

						parentFragmentManager.popBackStack()
					}
				}
			}
		}

	companion object {
		const val ARG_SERVER_UUID = "server_uuid"
	}
}
