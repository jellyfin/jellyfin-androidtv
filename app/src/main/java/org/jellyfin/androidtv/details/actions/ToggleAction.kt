package org.jellyfin.androidtv.details.actions

import android.content.Context

abstract class ToggleAction(id: Long, context: Context) : Action(id, context) {
	abstract var active: Boolean
}
