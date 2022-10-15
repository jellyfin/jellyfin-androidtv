package org.jellyfin.androidtv.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jellyfin.androidtv.ui.playback.AudioEventListener
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.extensions.ticks
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberMediaItem(
	mediaManager: MediaManager = koinInject(),
): Pair<BaseItemDto?, Float> {
	var progress by remember { mutableFloatStateOf(0f) }
	var item by remember { mutableStateOf(mediaManager.currentAudioItem) }

	DisposableEffect(mediaManager) {
		val listener = object : AudioEventListener {
			override fun onPlaybackStateChange(newState: PlaybackController.PlaybackState, currentItem: BaseItemDto?) {
				item = currentItem
			}

			override fun onQueueStatusChanged(hasQueue: Boolean) {
				super.onQueueStatusChanged(hasQueue)

				item = mediaManager.currentAudioItem
			}

			override fun onProgress(pos: Long) {
				val duration = item?.runTimeTicks?.ticks ?: Duration.ZERO
				progress = (pos.milliseconds / duration).toFloat()
			}
		}
		mediaManager.addAudioEventListener(listener)
		onDispose { mediaManager.removeAudioEventListener(listener) }
	}

	return item to progress
}
