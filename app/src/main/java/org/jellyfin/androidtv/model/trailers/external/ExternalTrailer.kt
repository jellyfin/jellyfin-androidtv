package org.jellyfin.androidtv.model.trailers.external

abstract class ExternalTrailer(val name: String, val playbackURL: String) {
	abstract val thumbnailURL: String?
}
