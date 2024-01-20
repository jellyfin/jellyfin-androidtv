package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class AudioTranscodeTarget(
		override val nameRes: Int,
) : PreferenceEnum {
	/**
	 *  Enable all codecs
	 */
	AUTO(R.string.lbl_audiotarget_auto),

	PCM(R.string.lbl_codec_pcm),

	AAC(R.string.lbl_codec_aac),

	AC3(R.string.lbl_codec_ac3),

	EAC3(R.string.lbl_codec_eac3),

	DTS(R.string.lbl_codec_dts)
}
