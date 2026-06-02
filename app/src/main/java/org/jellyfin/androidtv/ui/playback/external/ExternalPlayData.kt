package org.jellyfin.androidtv.ui.playback.external

import android.net.Uri
import org.jellyfin.sdk.model.api.MediaStream
import kotlin.time.Duration

data class ExternalPlayData(
	val url: Uri,
	val title: String,
	val fileName: String?,
	val externalSubtitles: List<Subtitle>,
	val position: Duration,
) {
	data class Subtitle(
		val mediaStream: MediaStream,

		val url: Uri,
		val name: String?,
		val language: String?
	)
}
