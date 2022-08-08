package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserPreferences.Companion.maxBitrate
import org.koin.java.KoinJavaComponent.get
import org.jellyfin.androidtv.constant.QualityProfiles

class VideoQualityController(
	@Suppress("unused")
	private val parentController: PlaybackController
) {
	companion object {
		private var previousQualitySelection = QualityProfiles.fromPreference(
				get<UserPreferences>(UserPreferences::class.java)
						.get(maxBitrate))
	}

	var currentQuality = previousQualitySelection
		@Suppress("unused")
		set(value) {
			val checkedVal = QualityProfiles.fromPreference(
				get<UserPreferences>(UserPreferences::class.java)
					.get(maxBitrate))

			previousQualitySelection = checkedVal
			field = checkedVal
		}

	init {
		currentQuality = previousQualitySelection
	}
}
