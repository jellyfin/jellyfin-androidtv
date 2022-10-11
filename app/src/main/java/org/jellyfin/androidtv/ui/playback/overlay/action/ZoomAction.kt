package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoManager
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment

class ZoomAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	init {
		initializeWithIcon(R.drawable.ic_aspect_ratio)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		leanbackOverlayFragment: LeanbackOverlayFragment,
		context: Context,
		view: View,
	) = PopupMenu(context, view, Gravity.END).apply {
		with(menu) {
			setGroupCheckable(0, true, false)

			add(
				0,
				VideoManager.ZOOM_AUTO_CROP,
				VideoManager.ZOOM_AUTO_CROP,
				context.getString(R.string.lbl_auto_crop)
			).apply {
				isChecked = playbackController.zoomMode == VideoManager.ZOOM_AUTO_CROP
			}

			add(
				0,
				VideoManager.ZOOM_FIT,
				VideoManager.ZOOM_FIT,
				context.getString(R.string.lbl_fit)
			).apply {
				isChecked = playbackController.zoomMode == VideoManager.ZOOM_FIT
			}

			add(
				0,
				VideoManager.ZOOM_STRETCH,
				VideoManager.ZOOM_STRETCH,
				context.getString(R.string.lbl_stretch)
			).apply {
				isChecked = playbackController.zoomMode == VideoManager.ZOOM_STRETCH
			}
		}

		setOnDismissListener { leanbackOverlayFragment.setFading(true) }
		setOnMenuItemClickListener { item ->
			playbackController.setZoom(item.itemId)
			true
		}
	}.show()
}
