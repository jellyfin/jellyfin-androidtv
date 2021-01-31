package org.jellyfin.androidtv.ui.startup

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.screen.AuthPreferencesScreen

class ManageServersScreen : OptionsFragment() {

	override val screen by optionsScreen {
		setTitle(R.string.lbl_manage_servers)

		link {
			setTitle(R.string.pref_authentication_link)
			icon = R.drawable.ic_users
			withFragment<AuthPreferencesScreen>()
		}
	}
}
