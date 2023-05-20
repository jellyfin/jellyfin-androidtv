package org.jellyfin.playback.core.mediasession

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.jellyfin.playback.core.queue.item.QueueEntryMetadata

fun QueueEntryMetadata.toMediaItem() = MediaItem.Builder().apply {
	if (mediaId != null) setMediaId(mediaId)

	setMediaMetadata(MediaMetadata.Builder().apply {
		setTitle(title)
		setArtist(artist)
		setAlbumTitle(albumTitle)
		setAlbumArtist(albumArtist)
		setDisplayTitle(displayTitle)
		setSubtitle(subtitle)
		setDescription(description)
		setArtworkUri(artworkUri?.toUri())
		setTrackNumber(trackNumber)
		setTotalTrackCount(totalTrackCount)
		recordDate?.let {
			setReleaseYear(it.year)
			setReleaseMonth(it.monthValue)
			setReleaseDay(it.dayOfMonth)
		}
		releaseDate?.let {
			setReleaseYear(it.year)
			setReleaseMonth(it.monthValue)
			setReleaseDay(it.dayOfMonth)
		}
		setWriter(writer)
		setComposer(composer)
		setConductor(conductor)
		setDiscNumber(discNumber)
		setTotalDiscCount(totalDiscCount)
		setGenre(genre)
		setCompilation(compilation)
	}.build())
}.build()
