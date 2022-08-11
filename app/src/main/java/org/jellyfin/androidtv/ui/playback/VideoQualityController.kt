package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.constant.QualityProfiles;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.koin.java.KoinJavaComponent;

class VideoQualityController(
	previousQualitySelection: QualityProfiles
) {

	var currentQuality = previousQualitySelection
	set(value) {
		KoinJavaComponent.get<UserPreferences>(UserPreferences::class.java)[UserPreferences.maxBitrate] =
			value.quality

		field = value
	}

	init {
		currentQuality = previousQualitySelection
	}
}
