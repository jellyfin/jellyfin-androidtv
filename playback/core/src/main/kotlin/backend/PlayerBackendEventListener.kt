package org.jellyfin.playback.core.backend

import org.jellyfin.playback.core.mediastream.PlayableMediaStream
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.QueueEntry

abstract class PlayerBackendEventListener {
	open fun onPlayStateChange(state: PlayState) = Unit
	open fun onVideoSizeChange(width: Int, height: Int) = Unit
	open fun onMediaStreamEnd(mediaStream: PlayableMediaStream) = Unit
	open fun onBuffering(entry: QueueEntry, buffering: Boolean) = Unit
	open fun onMediaStreamReady(entry: QueueEntry) = Unit
	open fun onMediaStreamError(entry: QueueEntry) = Unit
}
