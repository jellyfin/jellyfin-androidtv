package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.net.MalformedURLException
import java.net.URL

class YouTubeExternalTrailerLifter : ExternalTrailerLifter() {
	private val youtubeDomains = arrayListOf("youtube.com", "youtu.be")

	private fun isYoutubeUrl(url: URL) = youtubeDomains.contains(getDomain(url))

	override fun canLift(url: MediaUrl): Boolean {
		val parsedURL = try {
			URL(url.url)
		} catch (ex: MalformedURLException) {
			try {
				URL("https://" + url.url)
			} catch (ex: Exception) {
				return false
			}
		}

		return isYoutubeUrl(parsedURL)
	}

	override fun lift(url: MediaUrl): YouTubeTrailer {
		return YouTubeTrailer(url.name, url.url, "")
	}

}
