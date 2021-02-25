package org.jellyfin.androidtv.ui.startup

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.get

class ManageServersScreen : OptionsFragment() {
	init {
		rebuildOnResume = true
	}

	override val screen: OptionsScreen
		get() = optionsScreen {
			val authenticationStore = get<AuthenticationStore>()

			val servers = authenticationStore.getServers()

			setTitle(R.string.lbl_manage_servers)

			category {
				servers.forEach { server ->
					link {
						title = server.value.name
						icon = R.drawable.ic_cloud
						content = server.value.address
						withFragment<EditServerScreen>(bundleOf(
							EditServerScreen.ARG_SERVER_UUID to server.key
						))
					}
				}
			}
		}
}
