package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.itemtypes.Album
import org.jellyfin.androidtv.model.itemtypes.Artist
import org.jellyfin.androidtv.model.itemtypes.BaseItem
import org.jellyfin.androidtv.playback.MediaManager
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto

private const val LOG_TAG = "PlaybackAction"

abstract class PlaybackAction : Action {
	protected suspend fun playItem(context: Context, item: BaseItem, pos: Long, shuffle: Boolean) = withContext(Dispatchers.IO) {
		val baseItem = TvApp.getApplication().apiClient.getItem(item.id)

		if (baseItem == null) {
			Log.e(LOG_TAG, "Failed to get a base item for the given ID")
			return@withContext
		}

		PlaybackHelper.getItemsToPlay(baseItem, pos == 0L, shuffle, object : Response<List<BaseItemDto>>() {
			override fun onResponse(response: List<BaseItemDto>?) {
				if (item is Artist || item is Album) {
					// Music
					MediaManager.playNow(response)
				} else {
					// Video
					MediaManager.setCurrentVideoQueue(response)
					val cls = TvApp.getApplication().getPlaybackActivityClass(baseItem.baseItemType)
					val intent = Intent(context, cls).apply {
						putExtra("Position", pos)
					}
					context.startActivity(intent)
				}
			}
		});
	}
}
