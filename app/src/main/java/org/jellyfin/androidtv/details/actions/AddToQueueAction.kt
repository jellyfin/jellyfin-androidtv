package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.playback.MediaManager
import org.jellyfin.androidtv.util.apiclient.getItem

private const val LOG_TAG = "AddToQueueAction"

class AddToQueueAction(context: Context, private val item: BaseItem) : BaseAction(ActionID.ADD_TO_QUEUE.id, context) {
	init {
		label1 = context.getString(R.string.lbl_add_to_queue)
		icon = context.getDrawable(R.drawable.ic_add)
	}

	override fun onClick() {
		// TODO if audio is implemented, check here wheter we got an audio file and add it to audio queue
		GlobalScope.launch(Dispatchers.Main) {
			val retrieved = TvApp.getApplication().apiClient.getItem(item.id)
			if (retrieved == null) {
				Log.e(LOG_TAG, "Failed adding item %s to queue because retrieval failed!".format(item.id))
			} else {
				MediaManager.addToVideoQueue(retrieved)
			}
		}
	}

}
