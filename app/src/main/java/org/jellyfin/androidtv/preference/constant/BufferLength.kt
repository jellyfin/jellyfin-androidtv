package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class BufferLength(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Use Media3 default buffer durations.
	 */
	AUTO(R.string.playback_buffer_auto),

	/**
	 * Larger buffer, suitable for moderate or variable connections.
	 */
	LARGE(R.string.playback_buffer_large),

	/**
	 * Maximum buffer, intended for slow or satellite connections.
	 */
	EXTRA_LARGE(R.string.playback_buffer_extra_large),
}
