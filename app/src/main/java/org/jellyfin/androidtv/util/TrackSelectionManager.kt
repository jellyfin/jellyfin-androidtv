package org.jellyfin.androidtv.util

import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

object TrackSelectionManager {
    private val selectedAudioTracks = mutableMapOf<UUID, Int?>()
    private val selectedSubtitleTracks = mutableMapOf<UUID, Int?>()

    fun setSelectedAudioTrack(itemId: UUID, trackIndex: Int?) {
        selectedAudioTracks[itemId] = trackIndex
    }

    fun setSelectedSubtitleTrack(itemId: UUID, trackIndex: Int?) {
        selectedSubtitleTracks[itemId] = trackIndex
    }

    fun getSelectedAudioTrack(itemId: UUID): Int? {
        return selectedAudioTracks[itemId]
    }

    fun getSelectedSubtitleTrack(itemId: UUID): Int? {
        return selectedSubtitleTracks[itemId]
    }

    fun clearSelections(itemId: UUID) {
        selectedAudioTracks.remove(itemId)
        selectedSubtitleTracks.remove(itemId)
    }

    fun clearAllSelections() {
        selectedAudioTracks.clear()
        selectedSubtitleTracks.clear()
    }
}
