package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.GenericTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl

class GenericTrailerLifter : BaseTrailerLifter() {
	override fun canLift(url: MediaUrl): Boolean {
		return true
	}

	override fun lift(url: MediaUrl): GenericTrailer {
		return GenericTrailer(url.name, url.url)
	}
}
