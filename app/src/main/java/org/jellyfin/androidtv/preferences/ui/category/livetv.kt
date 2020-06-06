package org.jellyfin.androidtv.preferences.ui.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.enums.PreferredVideoPlayer
import org.jellyfin.androidtv.preferences.ui.dsl.OptionsScreen
import org.jellyfin.androidtv.preferences.ui.dsl.checkbox
import org.jellyfin.androidtv.preferences.ui.dsl.enum

fun OptionsScreen.liveTvCategory(
	userPreferences: UserPreferences
) = category {
	setTitle(R.string.pref_live_tv_cat)

	enum<PreferredVideoPlayer> {
		setTitle(R.string.pref_media_player)
		bind(userPreferences, UserPreferences.liveTvVideoPlayer)
	}

	checkbox {
		setTitle(R.string.lbl_direct_stream_live)
		bind(userPreferences, UserPreferences.liveTvDirectPlayEnabled)
	}
}
