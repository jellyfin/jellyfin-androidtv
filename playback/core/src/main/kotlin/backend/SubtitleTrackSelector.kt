package org.jellyfin.playback.core.backend

import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.playback.core.model.SubtitleTrack

interface SubtitleTrackSelector {
	val tracks: StateFlow<List<SubtitleTrack>>

	fun select(trackId: String?)
}
