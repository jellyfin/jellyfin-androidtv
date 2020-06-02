package org.jellyfin.androidtv.preferences.ui.category

import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.enums.PreferredVideoPlayer
import org.jellyfin.androidtv.preferences.ui.dsl.*

fun PreferenceScreen.liveTvCategory(
	userPreferences: UserPreferences
) = category(R.string.pref_live_tv_cat) {
	enumPreference<PreferredVideoPlayer>(R.string.pref_media_player) {
		bindEnum(userPreferences, UserPreferences.liveTvVideoPlayer)
	}
	checkboxPreference(R.string.lbl_direct_stream_live) {
		bind(userPreferences, UserPreferences.liveTvDirectPlayEnabled)
	}
}
