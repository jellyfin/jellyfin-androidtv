package org.jellyfin.playback.core.queue.item

/**
 * The QueueEntry is a single entry in a queue. It can represent any supported media type.
 * Implementations normally extend one of it's subclasses [BrandedQueueEntry] or [UserQueueEntry].
 */
interface QueueEntry {
	/**
	 * Whether this entry can be skipped and seeked or not. Normally set to `true`.
	 */
	val skippable: Boolean

	/**
	 * The metadata for this item.
	 */
	val metadata: QueueEntryMetadata
}

/**
 * Branded queue entries are used for intros, trailers, advertisements and other related videos. It
 * is not possible to seek or skip these items and are normally invisible when showing a queue's
 * contents.
 */
open class BrandedQueueEntry : QueueEntry {
	override val skippable = false
	override val metadata = QueueEntryMetadata.Empty
}

/**
 * User queue entries are used for regular media like music tracks, movies and series episodes. This
 * is the entry type used most often.
 */
open class UserQueueEntry : QueueEntry {
	override val skippable = true
	override val metadata = QueueEntryMetadata.Empty
}
