package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.model.itemtypes.PlayableItem

private const val LOG_TAG = "PlayFromBeginningAction"

class PlayFromBeginningAction(context: Context, val item: PlayableItem) : PlaybackAction(ActionID.PLAY_FROM_BEGINNING.id, context) {
	override val visible = true
	override val text = context.getString(R.string.lbl_play)
	override val icon = context.getDrawable(R.drawable.ic_play)!!

	override fun onClick() {
		Log.i(LOG_TAG, "Play from Beginning clicked!")
		GlobalScope.launch(Dispatchers.Main) {
			playItem(item, 0, false)
		}
	}
}
