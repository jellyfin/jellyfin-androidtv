package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.androidtv.ui.player.base.PlayerOverlayLayout
import org.jellyfin.androidtv.ui.player.base.rememberVisibilityTimer
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.koin.compose.koinInject

@Composable
fun VideoPlayerOverlay(
	modifier: Modifier = Modifier,
	playbackManager: PlaybackManager = koinInject(),
) {
	val visibilityTimerState = rememberVisibilityTimer()

	val entry by rememberQueueEntry(playbackManager)
	val item = entry?.run { baseItemFlow.collectAsState(baseItem) }?.value

	PlayerOverlayLayout(
		modifier = modifier
			.focusable()
			.onPreviewKeyEvent {
				if (visibilityTimerState.visible) visibilityTimerState.show()
				false
			}
			.onKeyEvent {
				if (it.key == Key.Back && visibilityTimerState.visible) {
					visibilityTimerState.hide()
					true
				} else if (!it.nativeKeyEvent.isSystem && !visibilityTimerState.visible) {
					visibilityTimerState.show()
					true
				} else {
					false
				}
			},
		visible = visibilityTimerState.visible,
		header = {
			VideoPlayerHeader(
				item = item,
			)
		},
		controls = {
			VideoPlayerControls(
				playbackManager = playbackManager,
			)
		},
	)
}
