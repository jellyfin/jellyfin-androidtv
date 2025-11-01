package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class RepeatAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	companion object {
		const val INDEX_NONE = 0
		const val INDEX_REPEAT_ONE = 1
		const val INDEX_REPEAT_ALL = 2
	}

	init {
		val repeatIcon = ContextCompat.getDrawable(context, R.drawable.ic_loop)
		setDrawables(arrayOf(repeatIcon))
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		val currentMode = videoPlayerAdapter.getRepeatMode()
		
		val popupMenu = PopupMenu(context, view, Gravity.END)
		popupMenu.menu.add(0, INDEX_NONE, 0, context.getString(R.string.lbl_repeat_off)).apply {
			isChecked = currentMode == INDEX_NONE
		}
		popupMenu.menu.add(0, INDEX_REPEAT_ONE, 1, context.getString(R.string.lbl_repeat_one)).apply {
			isChecked = currentMode == INDEX_REPEAT_ONE
		}
		popupMenu.menu.add(0, INDEX_REPEAT_ALL, 2, context.getString(R.string.lbl_repeat_all)).apply {
			isChecked = currentMode == INDEX_REPEAT_ALL
		}
		
		popupMenu.menu.setGroupCheckable(0, true, true)
		
		popupMenu.setOnMenuItemClickListener { menuItem ->
			// Set the repeat mode based on selected item
			val targetMode = menuItem.itemId
			val currentRepeatMode = videoPlayerAdapter.getRepeatMode()
			
			// Toggle to reach the target mode
			while (videoPlayerAdapter.getRepeatMode() != targetMode) {
				videoPlayerAdapter.toggleRepeat()
			}
			
			true
		}
		
		popupMenu.show()
	}
}
