package org.jellyfin.playback.core.queue.order

import kotlin.math.min

internal class ShuffleOrderIndexProvider : OrderIndexProvider {
	private val nextIndices = mutableListOf<Int>()

	override fun reset() = nextIndices.clear()

	override fun provideIndices(
		amount: Int,
		size: Int,
		playedIndices: Collection<Int>,
		currentIndex: Int,
	): Collection<Int> {
		val remainingItemsSize = size - playedIndices.size
		return if (remainingItemsSize <= 0) {
			emptyList()
		} else {
			val remainingIndices = (0..size).filterNot {
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

	override fun notifyRemoved(index: Int) {
		nextIndices.removeAll { it == index }
		nextIndices.replaceAll { if (it > index) it - 1 else it }
	}

	override fun useNextIndex() {
		nextIndices.removeAt(0)
	}
}
