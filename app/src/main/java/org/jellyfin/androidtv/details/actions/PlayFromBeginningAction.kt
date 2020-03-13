package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.PlayableItem

private const val LOG_TAG = "PlayFromBeginningAction"

class PlayFromBeginningAction(private val context: Context, val item: LiveData<out PlayableItem>) : PlaybackAction() {
	override val visible = MutableLiveData(true)
	override val text = MutableLiveData(context.getString(R.string.lbl_play))
	override val icon = MutableLiveData(context.getDrawable(R.drawable.ic_play)!!)

	override suspend fun onClick(view: View) {
		Log.i(LOG_TAG, "Play from Beginning clicked!")

		val value = item.value ?: return
		playItem(context, value, 0, false)
	}
}
