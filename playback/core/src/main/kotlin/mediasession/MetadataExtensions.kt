package org.jellyfin.playback.core.mediasession

import android.graphics.BitmapFactory
import androidx.media2.common.MediaItem
import androidx.media2.common.MediaMetadata
import kotlinx.coroutines.coroutineScope
import org.jellyfin.playback.core.queue.item.QueueEntryMetadata
import java.net.URL

private fun QueueEntryMetadata.toMediaMetadataBuilder() = MediaMetadata.Builder().apply {
	if (album != null) putString(MediaMetadata.METADATA_KEY_ALBUM, album)
	if (albumArtist != null) putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, albumArtist)
	if (albumArtUri != null) putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, albumArtUri)
	if (artist != null) putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
	if (artUri != null) putString(MediaMetadata.METADATA_KEY_ART_URI, artUri)
	if (author != null) putString(MediaMetadata.METADATA_KEY_AUTHOR, author)
	if (compilation != null) putString(MediaMetadata.METADATA_KEY_COMPILATION, compilation)
	if (composer != null) putString(MediaMetadata.METADATA_KEY_COMPOSER, composer)
	if (date != null) putString(MediaMetadata.METADATA_KEY_DATE, date.toString())
	if (discNumber != null) putLong(MediaMetadata.METADATA_KEY_DISC_NUMBER, discNumber)
	if (displayDescription != null) putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, displayDescription)
	if (displayIconUri != null) putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, displayIconUri)
	if (displaySubtitle != null) putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, displaySubtitle)
	if (displayTitle != null) putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, displayTitle)
	if (duration != null) putLong(MediaMetadata.METADATA_KEY_DURATION, duration.inWholeMilliseconds)
	if (genre != null) putString(MediaMetadata.METADATA_KEY_GENRE, genre)
	if (mediaId != null) putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
	if (mediaUri != null) putString(MediaMetadata.METADATA_KEY_MEDIA_URI, mediaUri)
	if (numTracks != null) putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, numTracks)
	if (title != null) putString(MediaMetadata.METADATA_KEY_TITLE, title)
	if (trackNumber != null) putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, trackNumber)
	if (writer != null) putString(MediaMetadata.METADATA_KEY_WRITER, writer)
	if (year != null) putLong(MediaMetadata.METADATA_KEY_YEAR, year.value.toLong())
}

private suspend fun MediaMetadata.Builder.putBitmap(key: String, url: String): MediaMetadata.Builder = coroutineScope {
	putBitmap(key, BitmapFactory.decodeStream(URL(url).openStream()))
}

suspend fun QueueEntryMetadata.toMediaMetadataBuilderWithBitmaps() = toMediaMetadataBuilder().apply {
	if (albumArtUri != null) putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArtUri)
	if (artUri != null) putBitmap(MediaMetadata.METADATA_KEY_ART, artUri)
	if (displayIconUri != null) putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, displayIconUri)
}

fun QueueEntryMetadata.toMediaMetadata() = toMediaMetadataBuilder().build()
fun QueueEntryMetadata.toMediaItem() = MediaItem.Builder().apply {
	setMetadata(toMediaMetadata())
}.build()

suspend fun QueueEntryMetadata.toMediaMetadataWithBitmaps() = toMediaMetadataBuilderWithBitmaps().build()
suspend fun QueueEntryMetadata.toMediaItemWithBitmaps() = MediaItem.Builder().apply {
	setMetadata(toMediaMetadataWithBitmaps())
}.build()
