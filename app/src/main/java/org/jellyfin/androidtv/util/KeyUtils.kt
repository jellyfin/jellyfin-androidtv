package org.jellyfin.androidtv.util

import android.view.KeyEvent

inline class KeyCode(val keyCode: Int)

inline fun KeyCode.isSelect() = this.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || this.keyCode == KeyEvent.KEYCODE_ENTER

inline fun KeyCode.isNumber() = this.keyCode >= KeyEvent.KEYCODE_0 && this.keyCode <= KeyEvent.KEYCODE_9

inline fun KeyCode.isChannelSurfUp() = this.keyCode == KeyEvent.KEYCODE_PAGE_UP || this.keyCode == KeyEvent.KEYCODE_CHANNEL_UP
inline fun KeyCode.isChannelSurfDown() = this.keyCode == KeyEvent.KEYCODE_PAGE_DOWN || this.keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN
inline fun KeyCode.isChannelUp() = this.keyCode == KeyEvent.KEYCODE_CHANNEL_UP
inline fun KeyCode.isChannelDown() = this.keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN
inline fun KeyCode.isPageUp() = this.keyCode == KeyEvent.KEYCODE_PAGE_UP
inline fun KeyCode.isPageDown() = this.keyCode == KeyEvent.KEYCODE_PAGE_DOWN
