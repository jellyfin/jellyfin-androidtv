package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.net.MalformedURLException
import java.net.URL

abstract class BaseTrailerLifter {
	protected fun mediaUrlToUrl(mediaUrl: MediaUrl) = try {
		URL(mediaUrl.url)
	} catch (ex: MalformedURLException) {
		try {
			URL("https://" + mediaUrl.url)
		} catch (ex: Exception) {
			null
		}
	}

	abstract fun canLift(url: MediaUrl): Boolean
	abstract fun lift(url: MediaUrl): ExternalTrailer?
}
