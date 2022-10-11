package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue

class GuideAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue
) : CustomAction(context, customPlaybackTransportControlGlue) {
	init {
		initializeWithIcon(R.drawable.ic_guide)
	}
}
