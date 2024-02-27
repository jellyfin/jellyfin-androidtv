package org.jellyfin.androidtv.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject

@Composable
fun rememberMediaItem(
	mediaManager: MediaManager = koinInject(),
): State<BaseItemDto?> {
	val item = remember { mutableStateOf(mediaManager.currentAudioItem) }

	DisposableEffect(mediaManager) {
		val listener = object : AudioEventListener {
			override fun onPlaybackStateChange(newState: PlaybackController.PlaybackState, currentItem: BaseItemDto?) {
				item.value = currentItem
			}

			override fun onQueueStatusChanged(hasQueue: Boolean) {
				super.onQueueStatusChanged(hasQueue)

				item.value = mediaManager.currentAudioItem
			}
		}
		mediaManager.addAudioEventListener(listener)
		onDispose { mediaManager.removeAudioEventListener(listener) }
	}

	return item
}
