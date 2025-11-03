package org.jellyfin.playback.core.queue.order

internal interface OrderIndexProvider {
	/**
	 * Called when the queue changes. Used to reset internal state of the provider.
	 */
	fun reset() = Unit

	/**
	 * Collect the next [amount] of indices to play.
	 *
	 * @param amount The maximum amount of indices to retrieve. May be less if there are none left.
	 * @param size The size of the queue to generate indices for.
	 * @param playedIndices The previously played indices, this may include the [currentIndex].
	 * @param currentIndex The currently playing index or [org.jellyfin.playback.core.queue.Queue.INDEX_NONE].
	 *
	 * @return A collection no more than [amount] items of indices to play next.
	 */
	fun provideIndices(
		amount: Int,
		size: Int,
		playedIndices: Collection<Int>,
		currentIndex: Int,
	): Collection<Int>

	/**
	 * Called when a queue entry is removed and indices have updated. All indices higher then [index] must be subtracted by one.
	 */
	fun notifyRemoved(index: Int) = Unit

	/**
	 * Called when the next index returned by [provideIndices] is starting to play. Used to
	 * modify internal state for the provider.
	 */
	fun useNextIndex() = Unit
}
