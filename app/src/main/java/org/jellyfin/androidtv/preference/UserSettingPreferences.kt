package org.jellyfin.androidtv.preference

import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.preference.store.DisplayPreferencesStore
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.intPreference
import org.jellyfin.sdk.api.client.ApiClient

class UserSettingPreferences(
	api: ApiClient,
) : DisplayPreferencesStore(
	displayPreferencesId = "usersettings",
	api = api,
	app = "emby",
) {
	companion object {
		val skipBackLength = intPreference("skipBackLength", 10000)
		val skipForwardLength = intPreference("skipForwardLength", 30000)

		val homesection0 = enumPreference("homesection0", HomeSectionType.LIBRARY_TILES_SMALL)
		val homesection1 = enumPreference("homesection1", HomeSectionType.RESUME)
		val homesection2 = enumPreference("homesection2", HomeSectionType.RESUME_AUDIO)
		val homesection3 = enumPreference("homesection3", HomeSectionType.RESUME_BOOK)
		val homesection4 = enumPreference("homesection4", HomeSectionType.LIVE_TV)
		val homesection5 = enumPreference("homesection5", HomeSectionType.NEXT_UP)
		val homesection6 = enumPreference("homesection6", HomeSectionType.LATEST_MEDIA)
	}

	val homesections
		get() = listOf(homesection0, homesection1, homesection2, homesection3, homesection4, homesection5, homesection6)
			.map(::get)
			.filterNot { it == HomeSectionType.NONE }
}
