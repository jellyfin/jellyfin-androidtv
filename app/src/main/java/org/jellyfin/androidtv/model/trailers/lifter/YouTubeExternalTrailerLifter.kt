package org.jellyfin.androidtv.model.trailers.lifter

import android.net.Uri
import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl

class YouTubeExternalTrailerLifter : ExternalTrailerLifter() {
	private val youtubeDomains = arrayListOf("youtube.com", "youtu.be")

	private fun isYoutubeUrl(uri: Uri) = youtubeDomains.contains(getDomain(uri))
	private fun isYoutubeUrl(url: MediaUrl) = isYoutubeUrl(Uri.parse(url.url))

	override fun canLift(url: MediaUrl): Boolean {
		return isYoutubeUrl(url)
	}

	override fun lift(url: MediaUrl): YouTubeTrailer {
		return YouTubeTrailer(url.name, url.url, "")
	}

}
