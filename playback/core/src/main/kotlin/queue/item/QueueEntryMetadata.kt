package org.jellyfin.playback.core.queue.item

import java.time.LocalDate
import kotlin.time.Duration

/**
 * Metadata for a queue entry (media file) based on the MediaMetadata properties in androidx.media3.
 */
data class QueueEntryMetadata(
	val mediaId: String? = null,
	val duration: Duration? = null,
	val title: String? = null,
	val artist: String? = null,
	val albumTitle: String? = null,
	val albumArtist: String? = null,
	val displayTitle: String? = null,
	val subtitle: String? = null,
	val description: String? = null,
	val artworkUri: String? = null,
	val trackNumber: Int? = null,
	val totalTrackCount: Int? = null,
	val recordDate: LocalDate? = null,
	val releaseDate: LocalDate? = null,
	val writer: String? = null,
	val composer: String? = null,
	val conductor: String? = null,
	val discNumber: Int? = null,
	val totalDiscCount: Int? = null,
	val genre: String? = null,
	val compilation: String? = null,
) {
	companion object {
		val Empty = QueueEntryMetadata()
	}
}
