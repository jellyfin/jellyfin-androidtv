package org.jellyfin.androidtv.util

import android.view.KeyEvent

object KeyUtils {
	@JvmStatic
	fun isSelect(code: Int) = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER

	@JvmStatic
	fun isNumber(code: Int) = code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9

	@JvmStatic
	fun isChannelSurfUp(code: Int) = code == KeyEvent.KEYCODE_PAGE_UP || code == KeyEvent.KEYCODE_CHANNEL_UP
	@JvmStatic
	fun isChannelSurfDown(code: Int) = code == KeyEvent.KEYCODE_PAGE_DOWN || code == KeyEvent.KEYCODE_CHANNEL_DOWN
	@JvmStatic
	fun isChannelUp(code: Int) = code == KeyEvent.KEYCODE_CHANNEL_UP
	@JvmStatic
	fun isChannelDown(code: Int) = code == KeyEvent.KEYCODE_CHANNEL_DOWN
	@JvmStatic
	fun isPageUp(code: Int) = code == KeyEvent.KEYCODE_PAGE_UP
	@JvmStatic
	fun isPageDown(code: Int) = code == KeyEvent.KEYCODE_PAGE_DOWN
}
