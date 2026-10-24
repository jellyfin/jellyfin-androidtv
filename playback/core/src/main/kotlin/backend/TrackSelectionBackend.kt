package org.jellyfin.playback.core.backend

/**
 * Represents an available track in the media.
 */
data class PlayerTrack(
	val index: Int,
	val type: TrackType,
	val label: String?,
	val language: String?,
	val codec: String?,
	val isSelected: Boolean,
	// Internal indices for backend use
	val groupIndex: Int = 0,
	val trackIndex: Int = 0,
)

enum class TrackType {
	AUDIO,
	SUBTITLE,
}

/**
 * Interface for backends that support track selection.
 */
interface TrackSelectionBackend {
	/**
	 * Get all available tracks of the specified type.
	 */
	fun getAvailableTracks(type: TrackType): List<PlayerTrack>

	/**
	 * Select a track by index. Pass -1 to disable subtitles.
	 */
	fun selectTrack(type: TrackType, index: Int): Boolean
}
