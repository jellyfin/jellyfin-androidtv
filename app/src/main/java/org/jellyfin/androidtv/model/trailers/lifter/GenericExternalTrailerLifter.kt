package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.GenericTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl

class GenericExternalTrailerLifter : ExternalTrailerLifter() {
	override fun canLift(url: MediaUrl): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun lift(url: MediaUrl): GenericTrailer {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}
