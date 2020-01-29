package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.util.apiclient.markPlayed
import org.jellyfin.androidtv.util.apiclient.markUnplayed
import org.jellyfin.apiclient.model.dto.UserItemDataDto

private val LOG_TAG: String = "ToggleWatchedAction"

class ToggleWatchedAction (context: Context, val item: PlayableItem, private val toggleListener: () -> Unit) : BaseAction(ActionID.TOGGLE_WATCHED.id, context) {

	init {
	    label1 = if (item.played) context.getString(R.string.lbl_mark_unplayed) else context.getString(R.string.lbl_mark_played)
	}

	override fun onClick() {
		GlobalScope.launch(Dispatchers.Main) {
			val newData : UserItemDataDto? = if (item.played) {
				TvApp.getApplication().apiClient.markUnplayed(item.id, TvApp.getApplication().currentUser.id)
			} else {
				TvApp.getApplication().apiClient.markPlayed(item.id, TvApp.getApplication().currentUser.id, null)
			}

			if (newData == null) {
				Log.e(LOG_TAG, "Failed to mark item played / unplayed!")
				return@launch
			}

			newData.apply {
				item.playbackPositionTicks = playbackPositionTicks
				item.played = played
				label1 = if (item.played) context.getString(R.string.lbl_mark_unplayed) else context.getString(R.string.lbl_mark_played)
			}

			toggleListener.invoke()
		}
	}
}
