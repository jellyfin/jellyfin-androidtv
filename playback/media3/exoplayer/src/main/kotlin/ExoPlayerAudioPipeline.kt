package org.jellyfin.playback.media3.exoplayer

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

		// Re-create loudness enhancer for normalization gain
		loudnessEnhancer?.release()
		loudnessEnhancer = runCatching { LoudnessEnhancer(audioSessionId) }
			.onFailure { Timber.w(it, "Failed to create LoudnessEnhancer") }
			.getOrNull()

		// Re-apply current normalization gain
		applyGain()
	}

	private fun applyGain() {
		if (loudnessEnhancer == null) {
			Timber.d("LoudnessEnhancer is not initialized")
			return
		}

		val targetGain = normalizationGain
			// Convert to millibels
			?.times(100f)
			// Round to integer
			?.toInt()
			// Ignore if zero (so the enhancer will be disabled)
			?.takeIf { it != 0 }

		Timber.d("Applying gain (targetGain=$targetGain)")
		runCatching {
			loudnessEnhancer?.setTargetGain(targetGain ?: 0)
		}.onSuccess {
			loudnessEnhancer?.setEnabled(targetGain != null)
		}.onFailure { error ->
			Timber.e(error, "Failed to apply gain of $targetGain")
			loudnessEnhancer?.setEnabled(false)
		}
	}
}
