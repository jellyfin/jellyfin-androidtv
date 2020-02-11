package org.jellyfin.androidtv.preferences.enums

enum class AudioBehavior {
	/**
	 * Directly stream audio without any changes
	 */
	DIRECT_STREAM,

	/**
	 * Downnmix audio to stereo. Disables the AC3, EAC3 and AAC_LATM audio codecs.
	 */
	DOWNMIX_TO_STEREO
}
