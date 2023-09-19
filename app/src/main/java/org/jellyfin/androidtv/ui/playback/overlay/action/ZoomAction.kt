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
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class ZoomAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
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
		return PopupMenu(context, view, Gravity.END).apply {
			with(menu) {
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

				setGroupCheckable(0, true, true)
			}

			setOnDismissListener { videoPlayerAdapter.leanbackOverlayFragment.setFading(true) }
			setOnMenuItemClickListener { item ->
				playbackController.setZoom(item.itemId)
				true
			}
		}.show()
	}
}
