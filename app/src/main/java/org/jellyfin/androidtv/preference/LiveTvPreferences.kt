package org.jellyfin.androidtv.preference

import org.jellyfin.sdk.api.operations.DisplayPreferencesApi

class LiveTvPreferences(
	displayPreferencesApi: DisplayPreferencesApi,
) : DisplayPreferencesStore(
	displayPreferencesId = "livetv",
	displayPreferencesApi = displayPreferencesApi
) {
	companion object {
		val channelOrder = Preference.string("livetv-channelorder", "DatePlayed")
		val colorCodeGuide = Preference.boolean("guide-colorcodedbackgrounds", false)
		val favsAtTop = Preference.boolean("livetv-favoritechannelsattop", true)
		val showHDIndicator = Preference.boolean("guide-indicator-hd", false)
		val showLiveIndicator = Preference.boolean("guide-indicator-live", true)
		val showNewIndicator = Preference.boolean("guide-indicator-new", false)
		val showPremiereIndicator = Preference.boolean("guide-indicator-premiere", true)
		val showRepeatIndicator = Preference.boolean("guide-indicator-repeat", false)
	}
}
