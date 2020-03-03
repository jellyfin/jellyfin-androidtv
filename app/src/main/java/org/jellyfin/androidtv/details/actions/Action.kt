package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.graphics.drawable.Drawable
import kotlin.properties.Delegates.observable

typealias ActionChangeListener = () -> Unit

abstract class Action(val id: Long, protected val context: Context) {
	abstract val visible: Boolean
	abstract val icon: Drawable
	abstract val text: String
//	open val description: String? = null

	abstract fun onClick()

	// Listener implementation
	private var changeListener: ActionChangeListener? = null

	fun setChangeListener(listener: ActionChangeListener?) {
		changeListener = listener
	}

	protected fun notifyDataChanged() {
		changeListener?.invoke()
	}
}
