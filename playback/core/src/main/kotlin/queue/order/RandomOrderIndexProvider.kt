package org.jellyfin.playback.core.queue.order

import org.jellyfin.playback.core.queue.Queue
import kotlin.random.Random

internal class RandomOrderIndexProvider : OrderIndexProvider {
	private val nextIndices = mutableListOf<Int>()

	override fun reset() = nextIndices.clear()

	override fun provideIndices(
		amount: Int,
		queue: Queue,
		playedIndices: Collection<Int>,
		currentIndex: Int,
	) = List(amount) { i ->
		if (i <= nextIndices.lastIndex) {
			nextIndices[i]
		} else {
			val index = Random.nextInt(queue.size)
			nextIndices.add(index)
			index
		}
	}

	override fun useNextIndex() {
		nextIndices.removeFirst()
	}
}
