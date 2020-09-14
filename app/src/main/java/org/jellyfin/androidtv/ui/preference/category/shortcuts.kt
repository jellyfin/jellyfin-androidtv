package org.jellyfin.androidtv.ui.preference.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.shortcut

fun OptionsScreen.shortcutsCategory(
	userPreferences: UserPreferences
) = category {
	setTitle(R.string.pref_button_remapping_category)

	shortcut {
		setTitle(R.string.pref_audio_track_button)
		bind(userPreferences, UserPreferences.shortcutAudioTrack)
	}

	shortcut {
		setTitle(R.string.pref_subtitle_track_button)
		bind(userPreferences, UserPreferences.shortcutSubtitleTrack)
	}
}
