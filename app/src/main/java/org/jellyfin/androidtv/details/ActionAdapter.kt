package org.jellyfin.androidtv.details

import androidx.leanback.widget.Action
import androidx.leanback.widget.ObjectAdapter

class ActionAdapter : ObjectAdapter() {
	private val actions = arrayListOf<Action>()

	fun reset() = actions.clear()

	fun add(action: Action) {
		actions += action
	}

	fun commit() = notifyChanged()

	override fun size() = actions.size
	override fun get(position: Int) = actions.getOrNull(position)
}
