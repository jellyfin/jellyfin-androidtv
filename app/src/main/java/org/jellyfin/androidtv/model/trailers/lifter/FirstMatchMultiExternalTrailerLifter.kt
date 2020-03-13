package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.util.*

class FirstMatchMultiExternalTrailerLifter : ExternalTrailerLifter() {
	private var lifters = ArrayDeque<ExternalTrailerLifter>()

	fun addFirst(lifter: ExternalTrailerLifter) {
		lifters.addFirst(lifter)
	}

	fun addLast(lifter: ExternalTrailerLifter) {
		lifters.addLast(lifter)
	}

	override fun canLift(url: MediaUrl): Boolean {
		return lifters.any { it.canLift(url) }
	}

	override fun lift(url: MediaUrl) : ExternalTrailer {
		for (lifter in lifters) {
			if (lifter.canLift(url)) {
				return lifter.lift(url)
			}
		}

		throw TrailerLiftingException("No lifter available that could handle %s".format(url.url))
	}
}
