package org.jellyfin.playback.core.model

data class VideoSize(
	val width: Int,
	val height: Int,
) {
	val aspectRatio: Float get() = width.toFloat() / height.toFloat()

	companion object {
		val EMPTY = VideoSize(0, 0)
	}
}
