package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Timeline
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.BaseTrackSelection
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.BandwidthMeter

@OptIn(UnstableApi::class)
class AdaptiveHdrTrackSelection(
	private val delegate: AdaptiveTrackSelection
) : BaseTrackSelection(
	delegate.trackGroup,
	IntArray(delegate.length()) { it },
	TYPE_UNSET
) {

	companion object {
		const val SELECTION_REASON_ADAPTIVE_HDR_UPGRADE = 1001
	}

	private var selectedIndex = 0

	override fun getSelectedIndex(): Int = selectedIndex

	override fun updateSelectedTrack(
		playbackPositionUs: Long,
		bufferedDurationUs: Long,
		availableDurationUs: Long,
		queue: List<MediaChunk>,
		mediaChunkIterators: Array<out MediaChunkIterator>
	) {
		delegate.updateSelectedTrack(
			playbackPositionUs,
			bufferedDurationUs,
			availableDurationUs,
			queue,
			mediaChunkIterators
		)

		upgradeSelectedToHdr(delegate.selectedIndex)?.let { selectedIndex = it }
	}

	override fun getSelectionReason(): Int =
		if (selectedIndex != delegate.selectedIndex) SELECTION_REASON_ADAPTIVE_HDR_UPGRADE
		else delegate.selectionReason

	override fun getSelectionData(): Any? = delegate.selectionData

	private fun upgradeSelectedToHdr(selected: Int): Int? {
		val currentFormat = getFormat(selected)

		if (currentFormat.hasHdrVideoRangeMetadata()) return selected

		return (0 until length)
			.firstOrNull { i ->
				val format = getFormat(i)

				format.bitrate == currentFormat.bitrate &&
					format.width == currentFormat.width &&
					format.height == currentFormat.height &&
					format.frameRate == currentFormat.frameRate &&
					format.hasHdrVideoRangeMetadata()
			}
	}

	class Factory(
		private val adaptiveFactory: AdaptiveTrackSelection.Factory =
			AdaptiveTrackSelection.Factory()
	) : ExoTrackSelection.Factory {

		override fun createTrackSelections(
			definitions: Array<out ExoTrackSelection.Definition?>,
			bandwidthMeter: BandwidthMeter,
			mediaPeriodId: MediaSource.MediaPeriodId,
			timeline: Timeline
		): Array<ExoTrackSelection?> {
			val baseSelections = adaptiveFactory.createTrackSelections(
				definitions,
				bandwidthMeter,
				mediaPeriodId,
				timeline
			)

			return baseSelections.map { base ->
				val adaptive = base as? AdaptiveTrackSelection
				if (adaptive?.trackGroup?.isVideo() == true)
					AdaptiveHdrTrackSelection(adaptive) else base
			}.toTypedArray()
		}
	}
}

@OptIn(UnstableApi::class)
private fun Format.hasHdrVideoRangeMetadata(): Boolean {
	val metadata = this.metadata ?: return false
	val hdrValues = listOf("PQ", "HLG")

	return (0 until metadata.length())
		.map { metadata[it] }
		.any { entry ->
			entry is VideoRangeHlsPlaylistParser.VideoRangeEntry &&
				hdrValues.any { value -> entry.value.equals(value, true) }
		}
}

@OptIn(UnstableApi::class)
private fun TrackGroup.isVideo(): Boolean =
	(0 until length).any {
		MimeTypes.isVideo(getFormat(it).sampleMimeType)
	}

