package org.jellyfin.androidtv.ui.player.video.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.player.base.toast.MediaToastRegistry
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState

@Composable
fun rememberPlaybackManagerMediaToastEmitter(
	playbackManager: PlaybackManager,
	mediaToastRegistry: MediaToastRegistry,
) {
	val coroutineScope = rememberCoroutineScope()

	LaunchedEffect(playbackManager) {
		var active = false

		playbackManager.state.playState
			.filter { playState ->
				// Wait until we received our first "playing" state before emitting play/pause events
				// this avoids an immediate toast from showing when the video player is opened
				if (!active) {
					active = playState == PlayState.PLAYING
					false
				} else {
					active
				}
			}
			.onEach { playState ->
				when (playState) {
					PlayState.PLAYING -> mediaToastRegistry.emit(R.drawable.ic_play)
					PlayState.PAUSED -> mediaToastRegistry.emit(R.drawable.ic_pause)
					else -> Unit
				}
			}
			.launchIn(coroutineScope)
	}
}
