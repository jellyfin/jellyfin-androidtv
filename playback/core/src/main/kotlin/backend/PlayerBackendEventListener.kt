package org.jellyfin.playback.core.backend

import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import kotlin.time.Duration

abstract class PlayerBackendEventListener {
	open fun onPlayStateChange(state: PlayState) = Unit
	open fun onVideoSizeChange(width: Int, height: Int) = Unit
	open fun onMediaStreamEnd(mediaStream: PlayableMediaStream, position: Duration) = Unit
}
