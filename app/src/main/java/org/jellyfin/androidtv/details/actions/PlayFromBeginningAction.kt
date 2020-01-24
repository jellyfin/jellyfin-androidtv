package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R

private const val LOG_TAG = "PlayFromBeginningAction"

class PlayFromBeginningAction(context: Context, val itemID: String) : PlaybackAction(ActionID.PLAY_FROM_BEGINNING.id, context) {
	init {
	    label1 = context.getString(R.string.lbl_play)
	}

	override fun onClick() {
		Log.i(LOG_TAG, "Play from Beginning clicked!")
		GlobalScope.launch(Dispatchers.Main) {
			playItemWithID(itemID, 0, false)
		}
	}
}
