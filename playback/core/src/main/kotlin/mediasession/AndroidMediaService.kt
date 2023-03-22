package org.jellyfin.playback.core.mediasession

import androidx.media2.session.MediaSession
import androidx.media2.session.MediaSessionService

class AndroidMediaService : MediaSessionService() {
	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = sessions.firstOrNull()
}
