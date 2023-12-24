package org.jellyfin.playback.core.queue.order

import org.jellyfin.playback.core.queue.Queue
import kotlin.math.min

internal class ShuffleOrderIndexProvider : OrderIndexProvider {
	private val nextIndices = mutableListOf<Int>()

	override fun reset() = nextIndices.clear()

	override fun provideIndices(
		amount: Int,
		queue: Queue,
		playedIndices: Collection<Int>,
		currentIndex: Int,
	): Collection<Int> {
		val remainingItemsSize = queue.size - playedIndices.size
		return if (remainingItemsSize <= 0) {
			emptyList()
		} else {
			val remainingIndices = (0..queue.size).filterNot {
				it in playedIndices || it in nextIndices
			}

			List(min(amount, remainingItemsSize)) { i ->
				if (i < nextIndices.lastIndex) {
					nextIndices[i]
				} else {
					val index = remainingIndices.random()
					nextIndices.add(index)
					index
				}
			}
		}
	}

	override fun useNextIndex() {
		nextIndices.removeFirst()
	}
}
