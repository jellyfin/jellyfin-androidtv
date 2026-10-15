package org.jellyfin.androidtv.data.compat

import java.util.UUID

class VideoOptions : AudioOptions() {
	var audioStreamIndex: Int? = null
	var subtitleStreamIndex: Int? = null
	var alwaysBurnInSubtitleWhenTranscoding: Boolean = false

	// A stream index belongs to one item, but survives same-item retries and refreshes.
	fun getAudioStreamIndex(itemId: UUID): Int? = audioStreamIndex.takeIf { this.itemId == itemId }
}
