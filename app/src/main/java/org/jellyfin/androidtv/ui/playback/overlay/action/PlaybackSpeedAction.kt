package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoSpeedController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment
import java.util.*

class PlaybackSpeedAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val speedController = VideoSpeedController(playbackController)
	private val speeds = VideoSpeedController.Companion.SpeedSteps.values()

	init {
		initializeWithIcon(R.drawable.exo_ic_speed)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		leanbackOverlayFragment: LeanbackOverlayFragment,
		context: Context, view: View
	) {
		val speedMenu = populateMenu(context, view, speedController)

		speedMenu.setOnDismissListener { leanbackOverlayFragment.setFading(true) }

		speedMenu.setOnMenuItemClickListener { menuItem ->
			speedController.setNewSpeed(speeds[menuItem.itemId])
			speedMenu.dismiss()
			return@setOnMenuItemClickListener true
		}

		speedMenu.show()
	}

	private fun populateMenu(
		context: Context,
		view: View,
		speedController: VideoSpeedController
	): PopupMenu {
		val speedMenu = PopupMenu(context, view, Gravity.END)
		val menu = speedMenu.menu
		speeds.forEachIndexed { i, selected ->
			// Since this is purely numeric data, coerce to en_us to keep the linter happy
			menu.add(0, i, i, String.format(Locale.US, "%.2fx", selected.speed))
		}

		menu.setGroupCheckable(0, true, true)
		menu.getItem(speeds.indexOf(speedController.getCurrentSpeed())).isChecked = true
		return speedMenu
	}


}
