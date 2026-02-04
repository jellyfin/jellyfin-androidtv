package org.jellyfin.playback.core.mediastream.mediatype

/**
 * Definition of the type for a specific media.
 */
enum class MediaType {
	/**
	 * Media type for primarily video content like movies, tv shows and live tv.
	 */
	Video,

	/**
	 * Media type for primarily audio content like music, podcasts and audio books.
	 */
	Audio,

	/**
	 * Unknown media type.
	 */
	Unknown,
}
