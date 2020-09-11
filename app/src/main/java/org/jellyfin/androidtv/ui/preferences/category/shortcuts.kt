package org.jellyfin.androidtv.ui.preferences.category

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.ui.preferences.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preferences.dsl.shortcut

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
