package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue

class ChapterAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	init {
		initializeWithIcon(R.drawable.ic_select_chapter)
	}
}
