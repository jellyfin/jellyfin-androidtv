package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.UserPreferences;

class VideoQualityController(
	previousQualitySelection: String,
	userPreferences: UserPreferences
) {

	var userPreferences = userPreferences
	var currentQuality = previousQualitySelection
	set(value) {
		userPreferences[UserPreferences.maxBitrate] = value

		field = value
	}

	init {
		currentQuality = previousQualitySelection
	}
}
