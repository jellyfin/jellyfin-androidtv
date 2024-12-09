package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class AudioBehavior(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Directly stream audio without any changes
	 */
	DIRECT_STREAM(R.string.pref_audio_direct),

	/**
	 * Downnmix audio to stereo. Disables the AC3, EAC3 and AAC_LATM audio codecs.
	 */
	DOWNMIX_TO_STEREO(R.string.pref_audio_compat),

	/**
	 * Stream AC3, EAC3, and DTS directly. Disables TrueHD and caps other codecs to stereo.
	 */
	HDMI_ARC_OUTPUT(R.string.audio_hdmi_arc)
}
