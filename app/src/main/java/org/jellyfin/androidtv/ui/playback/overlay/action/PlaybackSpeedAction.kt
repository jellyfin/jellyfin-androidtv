package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import androidx.core.util.Consumer
import androidx.leanback.widget.Action
import org.jellyfin.androidtv.customer.CustomerUserPreferences
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoSpeedController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import java.util.Locale

class PlaybackSpeedAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController,
	private val buttonRefresher: Consumer<Action?>,
	customerUserPreferences: CustomerUserPreferences,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val speedController = VideoSpeedController(playbackController)
	private val speeds = VideoSpeedController.SpeedSteps.entries.toTypedArray()
	private var popup: PopupMenu? = null
	private val customerUserPreferences = customerUserPreferences

	init {
		val currentSpeed = speedController.getEnumBySpeed(customerUserPreferences.videoSpeed)
		speedController.currentSpeed = currentSpeed
		initializeWithIcon(speedController.currentSpeed.icon)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()
		popup = populateMenu(context, view, speedController)

		popup?.setOnDismissListener {
			videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
			popup = null
		}

		popup?.setOnMenuItemClickListener { menuItem ->
			val speedSteps = speeds[menuItem.itemId]
			speedController.currentSpeed = speedSteps
			customerUserPreferences.videoSpeed = speedSteps.speed
			initializeWithIcon(speedController.currentSpeed.icon)
			buttonRefresher.accept(this)
			true
		}

		popup?.show()
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

	fun dismissPopup() {
		popup?.dismiss()
	}
}
