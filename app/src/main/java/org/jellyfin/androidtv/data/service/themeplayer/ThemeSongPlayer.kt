package org.jellyfin.androidtv.data.service.themeplayer

import androidx.annotation.MainThread
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PlaybackOrder
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.core.queue.QueueEntryPlaybackKind
import org.jellyfin.playback.core.queue.isThemePlayback
import org.jellyfin.playback.core.queue.playbackKind
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.core.queue.supplier.QueueSupplier
import org.jellyfin.playback.jellyfin.queue.createBaseItemQueueEntry
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ExtraType
import org.jellyfin.sdk.model.api.MediaType
import java.util.UUID

/**
 * Handles playing and stopping theme songs upon request. Not all play requests result in actual playback. Specifically, there will be no
 * playback if:
 * - Another audio media (that doesn't have the [ExtraType.THEME_SONG] type) is queued or currently playing.
 * - There are multiple sequential play requests for the same theme. Only the first request is honoured. Subsequent play requests for the
 *   same theme song will only work if a different theme song is played next, or if [stopThemeSong] is called.
 */
class ThemeSongPlayer(
	private val playbackManager: PlaybackManager,
	private val api: ApiClient,
) {
	private var allowedToPlay = false
	private var lastPlayedThemeSongUuid: UUID? = null

	fun prepareForPlay() {
		allowedToPlay = true
	}

	@MainThread
	fun playThemeSong(themeSong: BaseItemDto?) {
		// Item doesn't have a theme song. Stop any already playing theme songs.
		if (themeSong == null || themeSong.extraType != ExtraType.THEME_SONG) {
			stopThemeSong()
			return
		}

		if (!allowedToPlay || nonThemeMusicPlayingOrQueued())
			return

		// Theme song is already playing or was the last thing played. Don't start it again.
		if (lastPlayedThemeSongUuid == themeSong.id) {
			return
		}

		lastPlayedThemeSongUuid = themeSong.id

		val entry = createBaseItemQueueEntry(api, themeSong).apply {
			playbackKind = QueueEntryPlaybackKind.ThemeSong
		}

		playbackManager.queue.clear()
		playbackManager.state.setPlaybackOrder(PlaybackOrder.DEFAULT)
		playbackManager.queue.addSupplier(ThemeSongQueueSupplier(entry))
		playbackManager.state.play()
	}

	@MainThread
	fun stopThemeSong() {
		allowedToPlay = false
		lastPlayedThemeSongUuid = null

		if (playbackManager.queue.entry.value?.isThemePlayback == true) {
			playbackManager.state.stop()
		}
	}

	private fun nonThemeMusicPlayingOrQueued(): Boolean {
		val currentEntry = playbackManager.queue.entry.value ?: return playbackManager.state.playState.value != PlayState.STOPPED
		if (currentEntry.isThemePlayback) return false

		val baseItem = currentEntry.baseItem
		return when (baseItem?.mediaType) {
			MediaType.AUDIO -> true
			else -> playbackManager.state.playState.value != PlayState.STOPPED
		}
	}
}

private class ThemeSongQueueSupplier(
	private val entry: QueueEntry,
) : QueueSupplier {
	override val size: Int get() = 1

	override suspend fun getItem(index: Int): QueueEntry? = when (index) {
		0 -> entry
		else -> null
	}
}
