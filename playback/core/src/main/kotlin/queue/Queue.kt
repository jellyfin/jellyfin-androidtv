package org.jellyfin.playback.core.queue

import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.playback.core.queue.supplier.QueueSupplier

interface Queue {
	companion object {
		const val INDEX_NONE = -1
	}

	/**
	 * Get an estimated size of the queue. This may be off when the used suppliers are guessing their size.
	 */
	val estimatedSize: Int

	/**
	 * Index of the currently playing entry, or -1 if none.
	 */
	val entryIndex: StateFlow<Int>

	/**
	 * Currently playing entry or null.
	 */
	val entry: StateFlow<QueueEntry?>

	/**
	 * The currently loaded queue entries in their original order. This contains at least the currently playing entry and previous
	 * (unremoved) entries. Any upcoming entries are only added when they are at least peaked into using [peekNext] first.
	 */
	val entries: StateFlow<List<QueueEntry>>

	/**
	 * Add a supplier of queue items to the end of the queue. Will automatically fetch the first item if there is no current entry.
	 */
	fun addSupplier(supplier: QueueSupplier)

	/**
	 * Get all current suppliers of this queue.
	 */
	fun getSuppliers(): Collection<QueueSupplier>

	/**
	 * Clear all queue state, including suppliers, entries and currently playing entry.
	 */
	fun clear()

	/**
	 * Set the current entry to the previously played entry. Does nothing if there is no previous entry.
	 */
	suspend fun previous(): QueueEntry?

	/**
	 * Play the next entry in the queue.
	 *
	 * @param usePlaybackOrder Whether to use the playback order from the [PlayerState]. Default to true.
	 * @param useRepeatMode Whether to use the repeat mode from the [PlayerState]. Default to false.
	 */
	suspend fun next(usePlaybackOrder: Boolean = true, useRepeatMode: Boolean = false): QueueEntry?

	/**
	 * Skip to the given index.
	 *
	 * @param index The index of the entry to play
	 * @param saveHistory Whether to save the current entry to the play history
	 */
	suspend fun setIndex(index: Int, saveHistory: Boolean = false): QueueEntry?

	/**
	 * Find the index of a given entry.
	 *
	 * @param entry The entry to find the index for
	 */
	fun indexOf(entry: QueueEntry): Int?


	/**
	 * Remove a given queue entry.
	 *
	 * @param entry The entry to remove
	 */
	suspend fun removeEntry(entry: QueueEntry)

	/**
	 * Get the previously playing entry or null if none.
	 */
	suspend fun peekPrevious(): QueueEntry?

	/**
	 * Get the next entry or null if none.
	 *
	 * @param usePlaybackOrder Whether to use the playback order from the [PlayerState]. Default to true.
	 * @param useRepeatMode Whether to use the repeat mode from the [PlayerState]. Default to false.
	 */
	suspend fun peekNext(
		usePlaybackOrder: Boolean = true,
		useRepeatMode: Boolean = false,
	): QueueEntry?

	/**
	 * Get the next n entries in the queue. Where n is the amount to fetch. The returned collection may be smaller or empty depending on
	 * the entries in the queue.
	 *
	 * @param usePlaybackOrder Whether to use the playback order from the [PlayerState]. Default to true.
	 * @param useRepeatMode Whether to use the repeat mode from the [PlayerState]. Default to false.
	 */
	suspend fun peekNext(
		amount: Int,
		usePlaybackOrder: Boolean = true,
		useRepeatMode: Boolean = false,
	): Collection<QueueEntry>
}
