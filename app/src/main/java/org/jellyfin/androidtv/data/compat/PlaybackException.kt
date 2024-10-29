package org.jellyfin.androidtv.data.compat

import org.jellyfin.sdk.model.api.PlaybackErrorCode

class PlaybackException : RuntimeException() {
	var errorCode = PlaybackErrorCode.NOT_ALLOWED
}
