package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.leanback.widget.PlaybackControlsRow
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

abstract class CustomAction(
	private val context: Context,
	private val customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : PlaybackControlsRow.MultiAction(0) {
	fun onCustomActionClicked(view: View?) {
		// We need a custom onClicked implementation for showing the popup
		customPlaybackTransportControlGlue.onCustomActionClicked(this, view)
	}

	fun initializeWithIcon(@DrawableRes resourceId: Int) {
		icon = ContextCompat.getDrawable(context, resourceId)
		setDrawables(arrayOf(icon))
	}

	open fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {}
}
