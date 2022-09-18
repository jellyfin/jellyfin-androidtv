package org.jellyfin.androidtv.ui.shared

import android.view.KeyEvent

fun interface KeyListener {
	fun onKeyUp(key: Int, event: KeyEvent): Boolean
}
