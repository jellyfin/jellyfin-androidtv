package org.jellyfin.playback.core.model

data class SubtitleTrack(
	val id: String,
	val label: String?,
	val language: String?,
	val selected: Boolean,
)
