package org.jellyfin.androidtv.details.actions

import androidx.lifecycle.LiveData

interface ToggleableAction : Action {
	val active: LiveData<Boolean>
}
