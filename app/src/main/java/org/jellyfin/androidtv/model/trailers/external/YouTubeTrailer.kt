package org.jellyfin.androidtv.model.trailers.external

class YouTubeTrailer(name: String, playbackURL: String, override val thumbnailURL: String) : ExternalTrailer(name, playbackURL)
