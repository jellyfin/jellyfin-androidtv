package org.jellyfin.playback.core.queue.order

import kotlin.random.Random

internal class RandomOrderIndexProvider : OrderIndexProvider {
	private val nextIndices = mutableListOf<Int>()

	override fun reset() = nextIndices.clear()

	override fun provideIndices(
		amount: Int,
		size: Int,
		playedIndices: Collection<Int>,
		currentIndex: Int,
	) = List(amount) { i ->
		if (i <= nextIndices.lastIndex) {
			nextIndices[i]
		} else {
			val index = Random.nextInt(size)
			nextIndices.add(index)
			index
		}
	}

	override fun useNextIndex() {
		nextIndices.removeAt(0)
	}
}
