package org.jellyfin.playback.core.queue.item

// Queue Items
sealed interface QueueEntry {
	val skippable: Boolean
	val metadata: QueueEntryMetadata?
}

/**
 * Branded queue items are used for intros, trailers, advertisements and other related videos
 * It is not possible to seek or skip these items and are normally invisible when showing a queue's contents
 */
abstract class BrandedQueueEntry : QueueEntry {
	override val skippable = false
}

open class UserQueueEntry : QueueEntry {
	override val skippable = true
	override val metadata = QueueEntryMetadata()
}
