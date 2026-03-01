package org.jellyfin.androidtv.ui.presentation

import androidx.leanback.widget.ObjectAdapter

class CircularObjectAdapter(
	private val delegate: ObjectAdapter,
) : ObjectAdapter(delegate.presenterSelector) {
	companion object {
		private const val MULTIPLIER = 10_000
	}

	val realSize: Int get() = delegate.size()

	override fun size(): Int = if (realSize == 0) 0 else realSize * MULTIPLIER

	override fun get(position: Int): Any = delegate.get(position % realSize)!!

	fun centerPosition(realIndex: Int): Int = (MULTIPLIER / 2) * realSize + realIndex
}
