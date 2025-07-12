package org.jellyfin.androidtv.ui.itemhandling

import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.playback.jellyfin.queue.baseItem

class AudioQueueBaseRowItem(
	val queueEntry: QueueEntry,
) : BaseItemDtoBaseRowItem(
	item = requireNotNull(queueEntry.baseItem) { "AudioQueueBaseRowItem requires the BaseItem to be set on QueueEntry" },
	staticHeight = true,
) {
	var playing: Boolean = false
}
