package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserPreferences.Companion.maxBitrate
import org.koin.java.KoinJavaComponent.get

class VideoQualityController(
	private val parentController: PlaybackController
) {

	enum class QualityProfiles(val quality: String) {
		Quality_120(quality = "120"),
		Quality_110(quality = "110"),
		Quality_100(quality = "100"),
		Quality_90(quality = "90"),
		Quality_80(quality = "80"),
		Quality_70(quality = "70"),
		Quality_60(quality = "60"),
		Quality_50(quality = "50"),
		Quality_40(quality = "40"),
		Quality_30(quality = "30"),
		Quality_20(quality = "20"),
		Quality_15(quality = "15"),
		Quality_10(quality = "10"),
		Quality_5(quality = "5"),
		Quality_3(quality = "3"),
		Quality_2(quality = "2"),
		Quality_1(quality = "1"),
		Quality_072(quality = "0.72"),
		Quality_042(quality = "0.42"),
		Quality_0(quality = "0");


		companion object {
			private val mapping = values().associateBy(QualityProfiles::quality)
			fun fromPreference(quality: String) = mapping[quality]
		}
	}

	companion object {
		private var previousQualitySelection = QualityProfiles.fromPreference(get<UserPreferences>(UserPreferences::class.java).get(maxBitrate))
	}

	var currentQuality = previousQualitySelection
		set(value) {
			val checkedVal = QualityProfiles.fromPreference(get<UserPreferences>(UserPreferences::class.java).get(maxBitrate))

			previousQualitySelection = checkedVal
			field = checkedVal
		}

	init {
		currentQuality = previousQualitySelection
	}
}
