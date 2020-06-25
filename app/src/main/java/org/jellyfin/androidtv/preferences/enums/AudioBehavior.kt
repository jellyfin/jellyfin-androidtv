package org.jellyfin.androidtv.preferences.enums

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.ui.dsl.EnumDisplayOptions

enum class AudioBehavior {
	/**
	 * Directly stream audio without any changes
	 */
	@EnumDisplayOptions(R.string.pref_audio_direct)
	DIRECT_STREAM,

	/**
	 * Downnmix audio to stereo. Disables the AC3, EAC3 and AAC_LATM audio codecs.
	 */
	@EnumDisplayOptions(R.string.pref_audio_compat)
	DOWNMIX_TO_STEREO
}
