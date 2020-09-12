package org.jellyfin.androidtv.ui.preference.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum

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
