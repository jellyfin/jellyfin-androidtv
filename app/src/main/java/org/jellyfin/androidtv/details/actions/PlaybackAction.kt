package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.util.PlaybackUtil
import org.jellyfin.androidtv.util.apiclient.getItem

private const val LOG_TAG = "PlaybackAction"

abstract class PlaybackAction(id: Long, context: Context) : Action(id, context) {
	// TODO: This is super fucked, but at least it works for now until we clean up more messes.
	protected suspend fun playItem(item: BaseItem, pos: Long, shuffle: Boolean) = withContext(Dispatchers.IO) {
		val baseItem = TvApp.getApplication().apiClient.getItem(item.id)
		if (baseItem == null) {
			Log.e(LOG_TAG, "Failed to get a base item for the given ID")
			return@withContext
		}
		PlaybackUtil.play(context, baseItem, pos, shuffle)
	}

}
