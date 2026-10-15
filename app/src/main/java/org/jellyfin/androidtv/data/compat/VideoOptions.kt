package org.jellyfin.androidtv.data.compat

import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import java.util.UUID

class VideoOptions : AudioOptions() {
	var audioStreamIndex: Int? = null
	var subtitleStreamIndex: Int? = null
	var alwaysBurnInSubtitleWhenTranscoding: Boolean = false

	// A stream index belongs to one item, but survives same-item retries and refreshes.
	fun getAudioStreamIndex(itemId: UUID): Int? = audioStreamIndex.takeIf { this.itemId == itemId }

	fun applyAudioLanguage(language: String?, streams: List<MediaStream>?) {
		// An explicit selection for this item takes priority over a remembered language.
		if (audioStreamIndex != null || language == null) return
		val stream = streams?.firstOrNull { it.type == MediaStreamType.AUDIO && it.language == language }
		if (stream != null) audioStreamIndex = stream.index
	}
}
