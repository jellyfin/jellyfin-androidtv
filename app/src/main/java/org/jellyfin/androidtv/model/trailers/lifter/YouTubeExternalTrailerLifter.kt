package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.net.URL

class YouTubeExternalTrailerLifter : ExternalTrailerLifter() {
	private val youtubeDomains = arrayListOf("youtube.com", "youtu.be")

	private fun isYoutubeUrl(url: URL) = youtubeDomains.contains(getDomain(url))

	private val videoParameterRegex = Regex("v=([a-zA-Z0-9]+)")

	override fun canLift(url: MediaUrl): Boolean {
		val converted = mediaUrlToUrl(url)
		return converted != null && isYoutubeUrl(converted)
	}

	override fun lift(url: MediaUrl): YouTubeTrailer {
		if (!canLift(url))
			throw IllegalArgumentException("URL ${url.url} is not supported")

		val convertedURL = mediaUrlToUrl(url)!!

		val videoKey = when (getDomain(convertedURL)!!) {
			"youtube.com" -> {
				val matchGroups = videoParameterRegex.find(convertedURL.query)?.groups
				if (matchGroups == null || matchGroups.size != 2) {
					throw IllegalArgumentException("URL $convertedURL is not supported")
				} else {
					matchGroups[1]!!.value
				}
			}

			"youtu.be" -> {
				convertedURL.path.subSequence(1, convertedURL.path.length)
			}

			else -> {
				throw IllegalArgumentException("URL $convertedURL is not supported")
			}
		}

		val normalizedYouTubeURL = "https://www.youtube.com/watch?v=$videoKey"
		val thumbnailURL = "https://i1.ytimg.com/vi/$videoKey/hqdefault.jpg"

		return YouTubeTrailer(url.name, normalizedYouTubeURL, thumbnailURL)

	}

}
