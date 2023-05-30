package org.jellyfin.playback.core.queue.order

import org.jellyfin.playback.core.queue.Queue
import kotlin.math.min

internal class DefaultOrderIndexProvider : OrderIndexProvider {
	override fun provideIndices(
		amount: Int,
		queue: Queue,
		playedIndices: Collection<Int>,
		currentIndex: Int,
	): Collection<Int> {
		// No need to use currentQueueNextIndices because we can efficiently calculate the next items
		val remainingItemsSize = queue.size - currentIndex - 1

		return if (remainingItemsSize <= 0) emptyList()
		else Array(min(amount, remainingItemsSize)) { i -> currentIndex + i + 1 }.toList()
	}
}
