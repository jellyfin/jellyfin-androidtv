package org.jellyfin.androidtv.ui.syncplay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jellyfin.playback.core.PlaybackCommandInterceptor
import org.jellyfin.playback.core.PlaybackSeekCommand
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayClient
import org.jellyfin.playback.jellyfin.syncplay.SyncPlayRequest
import kotlin.time.Duration

class SyncPlayMediaSessionInterceptor(
	private val syncPlay: SyncPlayClient,
	private val scope: CoroutineScope,
) : PlaybackCommandInterceptor {
	override fun setPlaying(playing: Boolean) = intercept {
		if (playing && !syncPlay.state.value.following) syncPlay.resume()
		else syncPlay.request(if (playing) SyncPlayRequest.Unpause else SyncPlayRequest.Pause)
	}

	override fun stop() = intercept { syncPlay.halt() }

	override fun seek(position: Duration, command: PlaybackSeekCommand) = intercept {
		val occurrence = syncPlay.state.value.queue?.let {
			it.playlist.getOrNull(it.playingItemIndex)?.playlistItemId
		}
		when (command) {
			PlaybackSeekCommand.POSITION -> syncPlay.request(
				SyncPlayRequest.Seek(position.inWholeMilliseconds.coerceAtLeast(0) * TICKS_PER_MILLISECOND)
			)
			PlaybackSeekCommand.PREVIOUS -> occurrence?.let { syncPlay.request(SyncPlayRequest.Previous(it)) }
			PlaybackSeekCommand.NEXT -> occurrence?.let { syncPlay.request(SyncPlayRequest.Next(it)) }
		}
	}

	// Playback speed/repeat/shuffle cannot mutate the local timeline while following a group.
	override fun setSpeed(@Suppress("UnusedParameter") speed: Float) = joined()
	override fun setShuffle(@Suppress("UnusedParameter") enabled: Boolean) = joined()
	override fun setRepeat(@Suppress("UnusedParameter") enabled: Boolean) = joined()

	private fun intercept(action: suspend () -> Unit): Boolean {
		if (!joined()) return false
		scope.launch { action() }
		return true
	}

	private fun joined() = syncPlay.state.value.group != null

	private companion object {
		const val TICKS_PER_MILLISECOND = 10_000L
	}
}
