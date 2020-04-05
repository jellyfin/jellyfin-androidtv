package org.jellyfin.androidtv.model.trailers.lifter

import android.util.Log
import org.jellyfin.androidtv.model.trailers.external.YouTubeTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.net.URL

class YouTubeTrailerLifter : BaseTrailerLifter() {
	private val LOG_TAG = "YouTubeLifter"

	private val youtubeDomains = arrayListOf("youtube.com", "youtu.be")

	private fun isYoutubeUrl(url: URL) = youtubeDomains.fold(false) { acc: Boolean, domain: String ->
		acc || url.host.endsWith(domain)
	}

	private val fullDomainVideoParameterRegex = Regex("v=([a-zA-Z0-9]{11})")
	private val shortDomainVideoParameterRegex = Regex("youtu\\.be/[./]*([a-zA-Z0-9]{11})\$")

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

		val videoKey = when {
			convertedURL.host.endsWith("youtube.com") -> {
				getFirstCaptureGroupOrNull(fullDomainVideoParameterRegex, convertedURL.query)
			}
			convertedURL.host.endsWith("youtu.be") -> {
				getFirstCaptureGroupOrNull(shortDomainVideoParameterRegex, convertedURL.toString())
			}
			else -> {
				null
			}
		}

		if (videoKey == null) {
			Log.e(LOG_TAG, "URL $convertedURL is not supported")
			return null
		}

		val normalizedYouTubeURL = "https://www.youtube.com/watch?v=$videoKey"
		val thumbnailURL = "https://i1.ytimg.com/vi/$videoKey/hqdefault.jpg"

		return YouTubeTrailer(url.name, normalizedYouTubeURL, thumbnailURL)
	}

	private fun getFirstCaptureGroupOrNull(regex: Regex, toSearch: String): String? {
		val matchGroups = regex.find(toSearch)?.groups
		if (matchGroups == null || matchGroups.size != 2) {
			return null
		} else {
			return matchGroups[1]!!.value
		}
	}

}
