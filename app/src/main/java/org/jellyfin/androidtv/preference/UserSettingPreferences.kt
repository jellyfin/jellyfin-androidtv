package org.jellyfin.androidtv.preference

import org.jellyfin.sdk.api.client.ApiClient

class UserSettingPreferences(
	api: ApiClient,
) : DisplayPreferencesStore(
	displayPreferencesId = "usersettings",
	api = api,
	app = "emby",
) {
	companion object {
		val skipBackLength = Preference.int("skipBackLength", 10000)
		val skipForwardLength = Preference.int("skipForwardLength", 30000)
	}
}
