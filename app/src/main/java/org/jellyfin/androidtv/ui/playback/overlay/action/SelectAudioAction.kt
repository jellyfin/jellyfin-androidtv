package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.PlaybackManager
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment

class SelectAudioAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	private val playbackManager: PlaybackManager,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	init {
		initializeWithIcon(R.drawable.ic_select_audio)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		leanbackOverlayFragment: LeanbackOverlayFragment,
		context: Context,
		view: View,
	) {
		val audioTracks = playbackManager.getInPlaybackSelectableAudioStreams(playbackController.currentStreamInfo)
			?: return
		val currentAudioIndex = playbackController.audioStreamIndex

		PopupMenu(context, view, Gravity.END).apply {
			with(menu) {
				setGroupCheckable(0, true, false)
				for (track in audioTracks) {
					add(0, track.index, track.index, track.displayTitle).apply {
						isChecked = currentAudioIndex == track.index
					}
				}
			}

			setOnDismissListener { leanbackOverlayFragment.setFading(true) }
			setOnMenuItemClickListener { item ->
				playbackController.switchAudioStream(item.itemId)
				true
			}
		}.show()
	}
}
