package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.UserPreferences;
import org.koin.java.KoinJavaComponent;

class VideoQualityController(
	previousQualitySelection: String
) {

	var currentQuality = previousQualitySelection
	set(value) {
		KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[UserPreferences.maxBitrate] =
			value

		field = value
	}

	init {
		currentQuality = previousQualitySelection
	}
}
