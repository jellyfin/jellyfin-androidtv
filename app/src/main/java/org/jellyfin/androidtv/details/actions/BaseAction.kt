package org.jellyfin.androidtv.details.actions

import android.content.Context
import androidx.leanback.widget.Action
import kotlin.properties.Delegates.observable

private const val LOG_TAG = "BaseAction"

abstract class BaseAction(id: Long, protected val context: Context) : Action(id) {
	var isVisible: Boolean by observable(true) {_, _, _ ->
		onVisibilityChanged?.invoke()
	}

	var onVisibilityChanged: (() -> Unit)? = null

	abstract fun onClick()

}

abstract class ToggleAction(id: Long, context: Context) : BaseAction(id, context) {
	var active: Boolean = false
}
