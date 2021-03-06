package org.jellyfin.androidtv.ui.preference.screen

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.category.authenticationCategory
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.startup.preference.EditServerScreen
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

class AuthPreferencesScreen : OptionsFragment() {
	private val userPreferences: UserPreferences by inject()
	private val authenticationStore: AuthenticationStore by inject()

	override val screen by lazyOptionsScreen {
		setTitle(R.string.pref_authentication_cat)

		authenticationCategory(userPreferences, get())

		category {
			setTitle(R.string.lbl_manage_servers)

			authenticationStore.getServers().forEach { server ->
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
