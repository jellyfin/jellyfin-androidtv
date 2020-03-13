package org.jellyfin.androidtv.details.actions

import androidx.lifecycle.LiveData

abstract class ToggleAction: Action() {
	abstract val active: LiveData<Boolean>
}
