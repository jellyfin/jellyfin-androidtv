package org.jellyfin.androidtv.details.actions

import android.graphics.drawable.Drawable
import android.view.View
import androidx.lifecycle.LiveData

interface Action {
	val visible: LiveData<Boolean>
	val text: LiveData<String>
	val icon: LiveData<Drawable>

	suspend fun onClick(view: View?)
}
