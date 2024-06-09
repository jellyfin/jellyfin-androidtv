package org.jellyfin.playback.exoplayer

import android.media.audiofx.LoudnessEnhancer
import timber.log.Timber

class ExoPlayerAudioPipeline {
	private var loudnessEnhancer: LoudnessEnhancer? = null
	var normalizationGain: Float? = null
		set(value) {
			Timber.d("Normalization gain changed to $value")
			field = value
			applyGain()
		}

	fun setAudioSessionId(audioSessionId: Int) {
		Timber.d("Audio session id changed to $audioSessionId")

		// Re-creare loudness enhancer for normalization gain
		loudnessEnhancer?.release()
		loudnessEnhancer = LoudnessEnhancer(audioSessionId)

		// Re-apply current normalization gain
		applyGain()
	}

	private fun applyGain() {
		val targetGain = normalizationGain
			// Convert to millibels
			?.times(100f)
			// Round to integer
			?.toInt()

		Timber.d("Applying gain (targetGain=$targetGain)")
		loudnessEnhancer?.setEnabled(targetGain != null)
		loudnessEnhancer?.setTargetGain(targetGain ?: 0)
	}
}
