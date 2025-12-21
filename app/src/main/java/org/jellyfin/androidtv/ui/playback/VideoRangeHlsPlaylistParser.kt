package org.jellyfin.androidtv.ui.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
import androidx.media3.exoplayer.upstream.ParsingLoadable
import java.io.InputStream

@OptIn(UnstableApi::class)
class VideoRangeHlsPlaylistParser(
	private val delegate: ParsingLoadable.Parser<HlsPlaylist>
) : ParsingLoadable.Parser<HlsPlaylist> {

	override fun parse(uri: Uri, inputStream: InputStream): HlsPlaylist {
		val bytes = inputStream.readBytes()
		val playlist = delegate.parse(uri, bytes.inputStream())

		if (playlist !is HlsMultivariantPlaylist) return playlist

		val text = bytes.toString(Charsets.UTF_8)
		val hasRange = text.contains("VIDEO-RANGE=")

		if (!hasRange) return playlist

		val videoRangeMap = extractVideoRanges(text)

		if (videoRangeMap.isEmpty()) return playlist

		return injectVideoRangeMetadata(playlist, videoRangeMap)
	}

	private fun extractVideoRanges(text: String): Map<String, String> {
		val regex = Regex(
			"""#EXT-X-STREAM-INF:.*?VIDEO-RANGE=([A-Za-z0-9_-]+).*?\n([^\n]+)""",
			RegexOption.DOT_MATCHES_ALL
		)

		return regex.findAll(text)
			.associate { match ->
				val (range, uri) = match.destructured
				uri.trim() to range
			}
	}

	private fun injectVideoRangeMetadata(
        playlist: HlsMultivariantPlaylist,
        videoRanges: Map<String, String>
	): HlsMultivariantPlaylist {
		val updatedVariants = playlist.variants.map { variant ->
			val url = variant.url.toString()

			val range = videoRanges.entries.firstOrNull {
				url.endsWith(it.key)
			}?.value ?: return@map variant

			val newFormat = variant.format.buildUpon()
				.setMetadata(
					mergeMetadata(
						variant.format.metadata,
						range
					)
				)
				.build()

			variant.copyWithFormat(newFormat)
		}

		return HlsMultivariantPlaylist(
            playlist.baseUri,
            playlist.tags,
            updatedVariants,
            playlist.videos,
            playlist.audios,
            playlist.subtitles,
            playlist.closedCaptions,
            playlist.muxedAudioFormat,
            playlist.muxedCaptionFormats,
            playlist.hasIndependentSegments,
            playlist.variableDefinitions,
            playlist.sessionKeyDrmInitData
        )
	}

	private fun mergeMetadata(existing: Metadata?, range: String): Metadata {
		val entry = VideoRangeEntry(range)
		return existing?.copyWithAppendedEntries(entry) ?: Metadata(entry)
	}

	data class VideoRangeEntry(val value: String) : Metadata.Entry

	@OptIn(UnstableApi::class)
	class Factory : HlsPlaylistParserFactory {
		override fun createPlaylistParser(): ParsingLoadable.Parser<HlsPlaylist> =
			VideoRangeHlsPlaylistParser(HlsPlaylistParser())

		override fun createPlaylistParser(
            multivariantPlaylist: HlsMultivariantPlaylist,
            previousMediaPlaylist: HlsMediaPlaylist?
		): ParsingLoadable.Parser<HlsPlaylist> =
			VideoRangeHlsPlaylistParser(
                HlsPlaylistParser(multivariantPlaylist, previousMediaPlaylist)
			)
	}
}
