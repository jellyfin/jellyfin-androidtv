package org.jellyfin.androidtv.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.apiclient.StreamHelper
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

object TrackSelectionHelper {
	
	fun showAudioTrackSelection(
		context: Context,
		mediaSource: MediaSourceInfo?,
		currentIndex: Int?,
		onTrackSelected: (Int?) -> Unit
	) {
		val audioStreams = StreamHelper.getAudioStreams(mediaSource)
		if (audioStreams.isEmpty()) return
		
		val trackNames = audioStreams.map { stream ->
			buildTrackName(stream)
		}
		
		val currentSelection = audioStreams.indexOfFirst { it.index == currentIndex }
		
		AlertDialog.Builder(context)
			.setTitle(R.string.lbl_audio)
			.setSingleChoiceItems(trackNames.toTypedArray(), currentSelection) { dialog, which ->
				val selectedStream = audioStreams[which]
				onTrackSelected(selectedStream.index)
				dialog.dismiss()
			}
			.setNegativeButton(R.string.btn_cancel, null)
			.show()
	}
	
	fun showSubtitleTrackSelection(
		context: Context,
		mediaSource: MediaSourceInfo?,
		currentIndex: Int?,
		onTrackSelected: (Int?) -> Unit
	) {
		val subtitleStreams = StreamHelper.getSubtitleStreams(mediaSource)
		val trackNames = mutableListOf<String>()
		val trackIndices = mutableListOf<Int?>()
		
		// Add "Off" option
		trackNames.add(context.getString(R.string.lbl_audio_track_off))
		trackIndices.add(null)
		
		// Add subtitle tracks
		subtitleStreams.forEach { stream ->
			trackNames.add(buildTrackName(stream))
			trackIndices.add(stream.index)
		}
		
		val currentSelection = if (currentIndex == null) 0 else trackIndices.indexOfFirst { it == currentIndex }
		
		AlertDialog.Builder(context)
			.setTitle(R.string.lbl_subtitles)
			.setSingleChoiceItems(trackNames.toTypedArray(), currentSelection) { dialog, which ->
				val selectedIndex = trackIndices[which]
				onTrackSelected(selectedIndex)
				dialog.dismiss()
			}
			.setNegativeButton(R.string.btn_cancel, null)
			.show()
	}
	
	fun getCurrentAudioTrackName(mediaSource: MediaSourceInfo?, currentIndex: Int?): String? {
		if (currentIndex == null) return null
		val audioStreams = StreamHelper.getAudioStreams(mediaSource)
		val currentStream = audioStreams.find { it.index == currentIndex }
		return currentStream?.let { buildTrackName(it) }
	}
	
	fun getCurrentSubtitleTrackName(mediaSource: MediaSourceInfo?, currentIndex: Int?): String? {
		if (currentIndex == null) return "Off"
		val subtitleStreams = StreamHelper.getSubtitleStreams(mediaSource)
		val currentStream = subtitleStreams.find { it.index == currentIndex }
		return currentStream?.let { buildTrackName(it) } ?: "Off"
	}
	
	private fun buildTrackName(stream: MediaStream): String {
		val parts = mutableListOf<String>()
		
		// Add language if available
		stream.language?.let { parts.add(it) }
		
		// Add codec if available
		stream.codec?.let { parts.add(it.uppercase()) }
		
		// Add channel layout for audio
		if (stream.type == MediaStreamType.AUDIO) {
			stream.channelLayout?.let { parts.add(it) }
		}
		
		// Add profile for audio
		if (stream.type == MediaStreamType.AUDIO) {
			stream.profile?.let { parts.add(it) }
		}
		
		// Add display title if available
		stream.displayTitle?.let { parts.add(it) }
		
		// Add title if no display title
		if (stream.displayTitle == null) {
			stream.title?.let { parts.add(it) }
		}
		
		// Add default/forced indicators
		if (stream.isDefault) parts.add("(Default)")
		if (stream.isForced) parts.add("(Forced)")
		
		return if (parts.isNotEmpty()) {
			parts.joinToString(" - ")
		} else {
			"Track ${stream.index}"
		}
	}
}
