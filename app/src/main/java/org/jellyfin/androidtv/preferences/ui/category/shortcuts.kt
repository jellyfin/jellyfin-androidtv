package org.jellyfin.androidtv.preferences.ui.category

import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.ui.dsl.bind
import org.jellyfin.androidtv.preferences.ui.dsl.category
import org.jellyfin.androidtv.preferences.ui.dsl.shortcutPreference

fun PreferenceScreen.shortcutsCategory(
	userPreferences: UserPreferences
) = category(R.string.pref_button_remapping_category) {
	shortcutPreference(R.string.pref_audio_track_button) {
		bind(userPreferences, UserPreferences.shortcutAudioTrack)
	}
	shortcutPreference(R.string.pref_subtitle_track_button) {
		bind(userPreferences, UserPreferences.shortcutSubtitleTrack)
	}
}
