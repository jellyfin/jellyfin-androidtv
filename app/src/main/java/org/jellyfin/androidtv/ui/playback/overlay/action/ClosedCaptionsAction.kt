package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.androidtv.ui.playback.setSubtitleIndex
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

class ClosedCaptionsAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: PopupMenu? = null

	init {
		initializeWithIcon(R.drawable.ic_select_subtitle)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		if (playbackController.currentStreamInfo == null) {
			Timber.w("StreamInfo null trying to obtain subtitles")
			Toast.makeText(context, "Unable to obtain subtitle info", Toast.LENGTH_LONG).show()
			return
		}

		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		removePopup()
		popup = PopupMenu(context, view, Gravity.END).apply {
			with(menu) {
				var order = 0
				add(0, -1, order++, context.getString(R.string.lbl_none)).apply {
					isChecked = playbackController.subtitleStreamIndex == -1
				}

				for (sub in playbackController.currentMediaSource.mediaStreams.orEmpty()) {
					if (sub.type != MediaStreamType.SUBTITLE) continue

					add(0, sub.index, order++, sub.displayTitle).apply {
						isChecked = sub.index == playbackController.subtitleStreamIndex
					}
				}

				setGroupCheckable(0, true, false)
			}
			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			setOnMenuItemClickListener { item ->
				playbackController.setSubtitleIndex(item.itemId)
				true
			}
		}
		popup?.show()
	}

	fun removePopup() {
		popup?.dismiss()
	}
}
