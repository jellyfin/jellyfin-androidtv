package org.jellyfin.androidtv.ui.preference.category

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.startup.preference.EditServerScreen

fun OptionsScreen.manageServersCategory(
	authenticationRepository: AuthenticationRepository
) = category {
	setTitle(R.string.lbl_manage_servers)

	authenticationRepository.getServers().forEach { server ->
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
