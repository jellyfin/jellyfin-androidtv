package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions
import org.jellyfin.androidtv.util.DeviceUtils

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

val defaultAudioBehavior = if (DeviceUtils.isChromecastWithGoogleTV()) AudioBehavior.DOWNMIX_TO_STEREO
	else AudioBehavior.DIRECT_STREAM
