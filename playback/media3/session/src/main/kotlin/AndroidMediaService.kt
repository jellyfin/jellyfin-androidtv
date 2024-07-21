package org.jellyfin.playback.media3.session

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class AndroidMediaService : MediaSessionService() {
	override fun onGetSession(
		controllerInfo: MediaSession.ControllerInfo,
	): MediaSession? = sessions.firstOrNull()
}
