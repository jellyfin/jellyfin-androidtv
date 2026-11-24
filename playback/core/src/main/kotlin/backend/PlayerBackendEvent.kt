package org.jellyfin.playback.core.backend

import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import kotlin.time.Duration

sealed interface PlayerBackendEvent {
	data class PlayStateChanged(val state: PlayState) : PlayerBackendEvent
	data class VideoSizeChanged(val width: Int, val height: Int) : PlayerBackendEvent
	data class MediaStreamEnded(val stream: PlayableMediaStream, val position: Duration) : PlayerBackendEvent
}
