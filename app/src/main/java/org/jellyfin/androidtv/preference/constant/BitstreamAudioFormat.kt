package org.jellyfin.androidtv.preference.constant

import androidx.media3.common.MimeTypes
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.preference.Preference

/**
 * Bitstream audio formats that the user can override in the device profile.
 */
enum class BitstreamAudioFormat(
	val nameRes: Int,
	val mimeType: String,
	val preference: Preference<BitstreamAudioMode>,
) {
	AC3(
		nameRes = R.string.lbl_bitstream_ac3,
		mimeType = MimeTypes.AUDIO_AC3,
		preference = UserPreferences.bitstreamAc3,
	),
	EAC3(
		nameRes = R.string.bitstream_eac3,
		mimeType = MimeTypes.AUDIO_E_AC3,
		preference = UserPreferences.bitstreamEac3,
	),
	DTS(
		nameRes = R.string.bitstream_dts,
		mimeType = MimeTypes.AUDIO_DTS,
		preference = UserPreferences.bitstreamDts,
	),
	TRUEHD(
		nameRes = R.string.bitstream_truehd,
		mimeType = MimeTypes.AUDIO_TRUEHD,
		preference = UserPreferences.bitstreamTrueHd,
	),
}
