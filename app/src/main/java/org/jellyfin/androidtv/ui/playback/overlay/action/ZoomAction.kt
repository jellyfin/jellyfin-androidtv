package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.constant.ZoomMode
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.androidtv.util.popupMenu

class ZoomAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: PopupMenu? = null

	init {
		initializeWithIcon(R.drawable.ic_aspect_ratio)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()
		popup = popupMenu(context, view, Gravity.END) {
			item(context.getString(R.string.lbl_fit)) {
				playbackController.setZoom(ZoomMode.FIT)
			}.apply {
				isChecked = playbackController.zoomMode == ZoomMode.FIT
			}

			item(context.getString(R.string.lbl_auto_crop)) {
				playbackController.setZoom(ZoomMode.AUTO_CROP)
			}.apply {
				isChecked = playbackController.zoomMode == ZoomMode.AUTO_CROP
			}

			item(context.getString(R.string.lbl_stretch)) {
				playbackController.setZoom(ZoomMode.STRETCH)
			}.apply {
				isChecked = playbackController.zoomMode == ZoomMode.STRETCH
			}
		}
		popup?.menu?.setGroupCheckable(0, true, true)
		popup?.setOnDismissListener {
			videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
			popup = null
		}
		popup?.show()
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
