package org.jellyfin.androidtv.ui.playback.segment

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class MediaSegmentAction(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Don't take any action for this segment.
	 */
	NOTHING(R.string.segment_action_nothing),

	/**
	 * Seek to the end of this segment (endTicks). If the duration of this segment is shorter than 1 second it should do nothing to avoid
	 * lagg. The skip action will only execute when playing over the segment start, not when seeking into the segment block.
	 */
	SKIP(R.string.segment_action_skip),
}
