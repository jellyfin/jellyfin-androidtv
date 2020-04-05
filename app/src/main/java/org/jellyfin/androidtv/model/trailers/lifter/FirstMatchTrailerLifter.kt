package org.jellyfin.androidtv.model.trailers.lifter

import org.jellyfin.androidtv.model.trailers.external.ExternalTrailer
import org.jellyfin.apiclient.model.entities.MediaUrl
import java.util.*

class FirstMatchTrailerLifter : BaseTrailerLifter() {
	private var lifters = ArrayDeque<BaseTrailerLifter>()

	fun addFirst(lifter: BaseTrailerLifter) {
		lifters.addFirst(lifter)
	}

	fun addLast(lifter: BaseTrailerLifter) {
		lifters.addLast(lifter)
	}

	override fun canLift(url: MediaUrl): Boolean {
		return lifters.any { it.canLift(url) }
	}

	override fun lift(url: MediaUrl) : ExternalTrailer? {
		for (lifter in lifters) {
			if (lifter.canLift(url)) {
				return lifter.lift(url)
			}
		}

		return null
	}
}
