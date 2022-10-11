package org.jellyfin.androidtv.data.compat

import org.jellyfin.apiclient.model.dlna.PlaybackErrorCode

class PlaybackException : RuntimeException() {
	var errorCode = PlaybackErrorCode.NotAllowed
}
