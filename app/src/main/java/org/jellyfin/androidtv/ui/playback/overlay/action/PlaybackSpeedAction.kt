package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoSpeedController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import java.util.Locale

class PlaybackSpeedAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val speedController = VideoSpeedController(playbackController)
	private val speeds = VideoSpeedController.SpeedSteps.entries.toTypedArray()

	init {
		initializeWithIcon(R.drawable.ic_playback_speed)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		val speedMenu = populateMenu(context, view, speedController)

		speedMenu.setOnDismissListener { videoPlayerAdapter.leanbackOverlayFragment.setFading(true) }

		speedMenu.setOnMenuItemClickListener { menuItem ->
			speedController.currentSpeed = speeds[menuItem.itemId]
			speedMenu.dismiss()
			true
		}

		speedMenu.show()
	}

	private fun populateMenu(
		context: Context,
		view: View,
		speedController: VideoSpeedController
	) = PopupMenu(context, view, Gravity.END).apply {
		speeds.forEachIndexed { i, selected ->
			// Since this is purely numeric data, coerce to en_us to keep the linter happy
			menu.add(0, i, i, String.format(Locale.US, "%.2fx", selected.speed))
		}

		menu.setGroupCheckable(0, true, true)
		menu.getItem(speeds.indexOf(speedController.currentSpeed)).isChecked = true
	}

}
