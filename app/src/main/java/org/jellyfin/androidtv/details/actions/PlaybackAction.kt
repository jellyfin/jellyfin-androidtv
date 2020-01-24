package org.jellyfin.androidtv.details.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.playback.MediaManager
import org.jellyfin.androidtv.util.apiclient.PlaybackHelperKt
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

private const val LOG_TAG = "PlaybackAction"

abstract class PlaybackAction(id: Long, context: Context) : BaseAction(id, context) {
	// TODO: This is super fucked, but at least it works for now until we clean up more messes.
	protected suspend fun playItemWithID(id: String, pos: Long, shuffle: Boolean) = withContext(Dispatchers.IO) {
		val baseItem = TvApp.getApplication().apiClient.getItem(id)
		if (baseItem == null) {
			Log.e(LOG_TAG, "Failed to get a base item for the given ID")
			return@withContext
		}
		play(baseItem, pos, shuffle)
	}

	private suspend fun play(item: BaseItemDto, pos: Long, shuffle: Boolean) {
		val toPlay = PlaybackHelperKt.getItemsToPlayCoroutine(item, pos == 0L && item.baseItemType == BaseItemType.Movie, shuffle)

		if (toPlay != null) {
			if (item.baseItemType == BaseItemType.MusicArtist) {
				MediaManager.playNow(toPlay)
			} else {
				val intent = Intent(context, TvApp.getApplication().getPlaybackActivityClass(item.baseItemType))
				MediaManager.setCurrentVideoQueue(toPlay)
				intent.putExtra("Position", pos.toInt())
				ContextCompat.startActivity(context, intent, null)
			}
		}
	}
}
