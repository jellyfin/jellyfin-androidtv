package org.jellyfin.androidtv.model.trailers.lifter

import android.util.Log
import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.net.URL

class YouTubeExternalTrailerLifter : ExternalTrailerLifter() {
	private val LOG_TAG = "YouTubeLifter"

	private val youtubeDomains = arrayListOf("youtube.com", "youtu.be")

	private fun isYoutubeUrl(url: URL) = youtubeDomains.contains(getDomain(url))

	private val videoParameterRegex = Regex("v=([a-zA-Z0-9]+)")

	override fun canLift(url: MediaUrl): Boolean {
		val converted = mediaUrlToUrl(url)
		return converted != null && isYoutubeUrl(converted)
	}

	override fun lift(url: MediaUrl): YouTubeTrailer? {
		if (!canLift(url)) {
			 Log.e(LOG_TAG, "URL ${url.url} is not supported")
			return null
		}

		val convertedURL = mediaUrlToUrl(url)!!

		val videoKey = when (getDomain(convertedURL)!!) {
			"youtube.com" -> {
				val matchGroups = videoParameterRegex.find(convertedURL.query)?.groups
				if (matchGroups == null || matchGroups.size != 2) {
					Log.e(LOG_TAG, "URL $convertedURL is not supported")
					return null
				} else {
					matchGroups[1]!!.value
				}
			}

			"youtu.be" -> {
				convertedURL.path.subSequence(1, convertedURL.path.length)
			}

			else -> {
				Log.e(LOG_TAG, "URL $convertedURL is not supported")
				return null
			}
		}

		val normalizedYouTubeURL = "https://www.youtube.com/watch?v=$videoKey"
		val thumbnailURL = "https://i1.ytimg.com/vi/$videoKey/hqdefault.jpg"

		return YouTubeTrailer(url.name, normalizedYouTubeURL, thumbnailURL)

	}

}
