package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.net.URL

abstract class ExternalTrailerLifter {
	private val domainRegex = Regex("""^(.*\.|)(.+\...+)$""")

	protected fun getDomain(url: URL): String? {
		return url.host?.let { host -> domainRegex.find(host)?.groups?.get(2)?.value }
	}


	abstract fun canLift(url: MediaUrl) : Boolean
	abstract fun lift(url: MediaUrl) : ExternalTrailer
}
