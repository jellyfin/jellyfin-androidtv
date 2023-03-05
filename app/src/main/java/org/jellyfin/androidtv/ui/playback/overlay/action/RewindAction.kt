package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import androidx.leanback.widget.PlaybackControlsRow
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class RewindAction(context: Context) : PlaybackControlsRow.RewindAction(context), AndroidAction {
	override fun onActionClicked(videoPlayerAdapter: VideoPlayerAdapter) =
		videoPlayerAdapter.rewind()
}
