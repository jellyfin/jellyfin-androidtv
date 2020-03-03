package org.jellyfin.androidtv.details.actions

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.PlayableItem
import org.jellyfin.androidtv.util.apiclient.markPlayed
import org.jellyfin.androidtv.util.apiclient.markUnplayed

class ToggleWatchedAction(context: Context, val item: PlayableItem) : ToggleAction(ActionID.TOGGLE_WATCHED.id, context) {
	override val visible = true
	override val text = context.getString(R.string.lbl_watched)
	override val icon = context.getDrawable(R.drawable.ic_watch)!!
	override var active = true

	override fun onClick() {
		GlobalScope.launch(Dispatchers.Main) {
			val apiClient = TvApp.getApplication().apiClient

			val response = if (item.played) apiClient.markUnplayed(item.id, TvApp.getApplication().currentUser.id)
			else apiClient.markPlayed(item.id, TvApp.getApplication().currentUser.id, null)

			response?.let {
				item.playbackPositionTicks = it.playbackPositionTicks
				item.played = it.played

				//todo update self
				notifyDataChanged()
			}
		}
	}
}
