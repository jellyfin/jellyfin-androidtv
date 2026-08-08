package org.jellyfin.androidtv.ui.presentation

import androidx.leanback.widget.ObjectAdapter

class CircularObjectAdapter(
	private val delegate: ObjectAdapter,
) : ObjectAdapter(delegate.presenterSelector) {
	companion object {
		private const val MULTIPLIER = 1_000
	}

	val realSize: Int get() = delegate.size()

	override fun size(): Int = if (realSize == 0) 0 else realSize * MULTIPLIER

	override fun get(position: Int): Any {
		val size = realSize
		if (size == 0) {
			throw IndexOutOfBoundsException("CircularObjectAdapter is empty")
		}

		return delegate.get(Math.floorMod(position, size))!!
	}

	fun centerPosition(realIndex: Int): Int {
		val size = realSize
		if (size == 0 || realIndex < 0 || realIndex >= size) return -1
		return (MULTIPLIER / 2) * size + realIndex
	}
}
