package org.jellyfin.androidtv.ui.startup

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.get

class ManageServersScreen : OptionsFragment() {

	override val screen by optionsScreen {
		val authenticationStore = get<AuthenticationStore>()

		val servers = authenticationStore.getServers()

		setTitle(R.string.lbl_manage_servers)

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
