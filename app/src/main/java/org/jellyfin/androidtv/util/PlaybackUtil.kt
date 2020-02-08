package org.jellyfin.androidtv.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.playback.MediaManager
import org.jellyfin.androidtv.util.apiclient.PlaybackHelperKt
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType

class PlaybackUtil {
	companion object {
		suspend fun play(context: Context, item: BaseItemDto, pos: Long, shuffle: Boolean) {
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
}
