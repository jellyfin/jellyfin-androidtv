package org.jellyfin.playback.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView
import android.widget.FrameLayout
import org.jellyfin.playback.core.PlaybackManager

/**
 * A view that is used to display the video output of the playing media.
 * The [playbackManager] must be set when the view is initialized.
 */
class PlayerSurfaceView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	lateinit var playbackManager: PlaybackManager

	val surface = SurfaceView(context, attrs).apply {
		addView(this, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		if (!isInEditMode) {
			playbackManager.backendService.attachSurfaceView(this)
		}
	}
}

