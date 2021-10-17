package org.jellyfin.androidtv.preference

import org.jellyfin.sdk.api.operations.DisplayPreferencesApi

class UserSettingPreferences(
	displayPreferencesApi: DisplayPreferencesApi,
) : DisplayPreferencesStore(
	displayPreferencesId = "usersettings",
	displayPreferencesApi = displayPreferencesApi,
	app = "emby",
) {
	companion object {
		val skipBackLength = Preference.int("skipBackLength", 10000)
		val skipForwardLength = Preference.int("skipForwardLength", 30000)
	}
}
