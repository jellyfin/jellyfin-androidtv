package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.UserPreferences;

class VideoQualityController(
	previousQualitySelection: String,
	private val userPreferences: UserPreferences,
) {
	var currentQuality = previousQualitySelection
		set(value) {
			userPreferences[UserPreferences.maxBitrate] = value
			field = value
		}
}
