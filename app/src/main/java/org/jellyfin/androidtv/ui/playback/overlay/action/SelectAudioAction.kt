package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class SelectAudioAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: PopupMenu? = null

	init {
		initializeWithIcon(R.drawable.ic_select_audio)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		val audioTracks = playbackController.currentStreamInfo?.selectableAudioStreams ?: return
		val currentAudioIndex = playbackController.audioStreamIndex

		dismissPopup()
		popup = PopupMenu(context, view, Gravity.END).apply {
			with(menu) {
				for (track in audioTracks) {
					add(0, track.index, track.index, track.displayTitle).apply {
						isChecked = currentAudioIndex == track.index
					}
				}
				setGroupCheckable(0, true, false)
			}

			setOnDismissListener {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			}
			setOnMenuItemClickListener { item ->
				playbackController.switchAudioStream(item.itemId)
				true
			}
		}
		popup?.show()
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
