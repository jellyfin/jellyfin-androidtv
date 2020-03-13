package org.jellyfin.androidtv.model.trailers.lifter

import android.net.Uri
import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl

abstract class ExternalTrailerLifter {
	private val domainRegex = Regex("""^(.*\.|)(.+\...+)$""")

	protected fun getDomain(uri: Uri): String? = uri.host?.let { host -> domainRegex.find(host)?.groups?.get(2)?.value }


	abstract fun canLift(url: MediaUrl) : Boolean
	abstract fun lift(url: MediaUrl) : ExternalTrailer
}
