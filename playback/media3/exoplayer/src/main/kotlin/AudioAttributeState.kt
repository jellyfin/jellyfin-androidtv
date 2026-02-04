package org.jellyfin.playback.media3.exoplayer

import androidx.media3.common.AudioAttributes
import timber.log.Timber

class AudioAttributeState {
	private var _audioAttributes: AudioAttributes? = null

	fun updateAudioAttributes(
		builder: AudioAttributes.Builder.() -> Unit,
		onChange: (audioAttributes: AudioAttributes) -> Unit,
	) {
		val newAudioAttributes = AudioAttributes.Builder().apply {
			builder()
		}.build()

		if (_audioAttributes != newAudioAttributes) {
			Timber.d("Audio attributes changed")

			onChange(newAudioAttributes)
			_audioAttributes = newAudioAttributes
		}
	}
}
