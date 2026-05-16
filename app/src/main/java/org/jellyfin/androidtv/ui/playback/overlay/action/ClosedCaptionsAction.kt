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
	companion object {
		private const val ITEM_SET_OFFSET = Int.MIN_VALUE
	}

	private var popup: PopupMenu? = null
	private val subtitleOffsetPopup = SubtitleOffsetPopup(context)

	init {
		initializeWithIcon(R.drawable.ic_select_subtitle)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		subtitleOffsetPopup.dismiss()

		if (playbackController.currentStreamInfo == null) {
			Timber.w("StreamInfo null trying to obtain subtitles")
			Toast.makeText(context, "Unable to obtain subtitle info", Toast.LENGTH_LONG).show()
			return
		}

		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		removePopup()
		var openingSubtitleOffsetPopup = false
		popup = PopupMenu(context, view, Gravity.END).apply {
			with(menu) {
				var order = 0

				if (videoPlayerAdapter.hasTimingAdjustableSubtitle()) {
					add(1, ITEM_SET_OFFSET, order++, context.getString(R.string.lbl_subtitle_offset))
				}

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
				if (!openingSubtitleOffsetPopup) {
					videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				}
				popup = null
			}
			setOnMenuItemClickListener { item ->
				if (item.itemId == ITEM_SET_OFFSET) {
					openingSubtitleOffsetPopup = true
					view.post {
						subtitleOffsetPopup.show(playbackController, videoPlayerAdapter)
					}
					false
				} else {
					playbackController.setSubtitleIndex(item.itemId)
					true
				}
			}
		}
		popup?.show()
	}

	fun removePopup() {
		popup?.dismiss()
		subtitleOffsetPopup.dismiss()
	}
}
