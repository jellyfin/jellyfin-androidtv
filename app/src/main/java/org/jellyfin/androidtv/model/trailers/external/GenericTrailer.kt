package org.jellyfin.androidtv.model.trailers.external

class GenericTrailer(name: String, playbackURL: String) : ExternalTrailer(name, playbackURL) {
	override val thumbnailURL: String? = null
}
