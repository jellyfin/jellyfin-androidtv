package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.element.ElementKey

/**
 * Describes how a [QueueEntry] should be treated by playback services.
 */
enum class QueueEntryPlaybackKind {
	Default,
	ThemeSong,
}

private val playbackKindKey = ElementKey<QueueEntryPlaybackKind>("QueueEntryPlaybackKind")

var QueueEntry.playbackKind: QueueEntryPlaybackKind
	get() = this.getOrNull(playbackKindKey) ?: QueueEntryPlaybackKind.Default
	set(value) {
		if (value == QueueEntryPlaybackKind.Default) {
			this.remove(playbackKindKey)
		} else {
			this.put(playbackKindKey, value)
		}
	}

val QueueEntry.isThemePlayback: Boolean
	get() = playbackKind == QueueEntryPlaybackKind.ThemeSong
