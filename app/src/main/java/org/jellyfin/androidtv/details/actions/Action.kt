package org.jellyfin.androidtv.details.actions

import android.graphics.drawable.Drawable
import android.view.View
import androidx.lifecycle.LiveData

abstract class Action {
	abstract val visible: LiveData<Boolean>
	abstract val text: LiveData<String>
	abstract val icon: LiveData<Drawable>

	abstract suspend fun onClick(view: View)
}
