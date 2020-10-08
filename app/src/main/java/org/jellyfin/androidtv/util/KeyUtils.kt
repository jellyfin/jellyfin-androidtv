package org.jellyfin.androidtv.util

import android.view.KeyEvent

fun isDPADCenter(keyCode: Int): Boolean {
	if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
		return true;
	return false;
}

fun isChannelUp(keyCode: Int): Boolean {
	if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP)
		return true;
	return false;
}

fun isChannelDown(keyCode: Int): Boolean {
	if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN)
		return true;
	return false;
}
