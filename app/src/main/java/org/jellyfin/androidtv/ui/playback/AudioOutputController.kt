package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.koin.java.KoinJavaComponent

class AudioOutputController(
	private val parentController: PlaybackController
) {

	enum class AudioOutputs(val output: AudioBehavior) {
		DIRECT_STREAM(output = AudioBehavior.DIRECT_STREAM),
		DOWNMIX_TO_STEREO(output = AudioBehavior.DOWNMIX_TO_STEREO);

		companion object {
			private val mapping = values().associateBy(AudioOutputs::output)
			fun fromPreference(behavior: AudioBehavior) = mapping[behavior]
		}
	}

	companion object {
		private var previousAudioOutputSelectioin = AudioOutputs.fromPreference(
			KoinJavaComponent.get<UserPreferences>(
				UserPreferences::class.java
			).get(UserPreferences.audioBehaviour))
	}

	var currentAudioOutput = previousAudioOutputSelectioin
		set(value) {
			val checkedVal = AudioOutputs.fromPreference(
				KoinJavaComponent.get<UserPreferences>(
					UserPreferences::class.java
				).get(UserPreferences.audioBehaviour))

			previousAudioOutputSelectioin = checkedVal
			field = checkedVal
		}

	init {
		currentAudioOutput = previousAudioOutputSelectioin
	}
}
