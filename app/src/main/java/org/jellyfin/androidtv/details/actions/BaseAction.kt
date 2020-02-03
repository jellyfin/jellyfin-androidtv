package org.jellyfin.androidtv.details.actions

import android.content.Context
import androidx.leanback.widget.Action

private const val LOG_TAG = "BaseAction"

abstract class BaseAction(id: Long, protected val context: Context) : Action(id) {
	abstract fun onClick()
}

abstract class ToggleAction(id: Long, context: Context) : BaseAction(id, context) {
	var active: Boolean = false
}
