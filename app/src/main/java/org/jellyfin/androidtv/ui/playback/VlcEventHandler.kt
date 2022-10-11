package org.jellyfin.androidtv.ui.playback

import org.videolan.libvlc.MediaPlayer

class VlcEventHandler : MediaPlayer.EventListener {
	var onCompletionListener: PlaybackListener? = null
	var onErrorListener: PlaybackListener? = null
	var onPreparedListener: PlaybackListener? = null
	var onProgressListener: PlaybackListener? = null

	override fun onEvent(event: MediaPlayer.Event) {
		when (event.type) {
			MediaPlayer.Event.EndReached -> onCompletionListener?.onEvent()
			MediaPlayer.Event.Playing -> onPreparedListener?.onEvent()
			MediaPlayer.Event.PositionChanged -> onProgressListener?.onEvent()
			MediaPlayer.Event.EncounteredError -> onErrorListener?.onEvent()
			else -> Unit
		}
	}
}
