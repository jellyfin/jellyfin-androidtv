package org.jellyfin.androidtv.ui.playback

import org.jellyfin.apiclient.model.dto.BaseItemDto

interface AudioEventListener {
	fun onPlaybackStateChange(newState: PlaybackController.PlaybackState, currentItem: BaseItemDto?) = Unit
	fun onProgress(pos: Long) = Unit
	fun onQueueStatusChanged(hasQueue: Boolean) = Unit
	fun onQueueReplaced() = Unit
}
