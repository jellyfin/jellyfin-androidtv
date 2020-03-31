package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.net.MalformedURLException
import java.net.URL

abstract class ExternalTrailerLifter {
	private val domainRegex = Regex("""^(.*\.|)(.+\...+)$""")

	protected fun getDomain(url: URL): String? {
		return url.host?.let { host -> domainRegex.find(host)?.groups?.get(2)?.value }
	}

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
