package org.jellyfin.androidtv.util

import android.os.Build
import android.view.KeyEvent

/**
 * Returns whether this key event is a media key event or not.
 */
fun KeyEvent.isMediaSessionKeyEvent(): Boolean = when {
	Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> KeyEvent.isMediaSessionKey(keyCode)

	else -> when (keyCode) {
		KeyEvent.KEYCODE_MEDIA_PLAY,
		KeyEvent.KEYCODE_MEDIA_PAUSE,
		KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
		KeyEvent.KEYCODE_HEADSETHOOK,
		KeyEvent.KEYCODE_MEDIA_STOP,
		KeyEvent.KEYCODE_MEDIA_NEXT,
		KeyEvent.KEYCODE_MEDIA_PREVIOUS,
		KeyEvent.KEYCODE_MEDIA_REWIND,
		KeyEvent.KEYCODE_MEDIA_RECORD,
		KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> true

		else -> false
	}
}
