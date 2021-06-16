package org.jellyfin.androidtv.ui.shared

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import org.jellyfin.androidtv.util.DeviceUtils

class KeyboardFocusChangeListener : View.OnFocusChangeListener {
	override fun onFocusChange(view: View, hasFocus: Boolean) {
		val inputMethodManager = view.context.getSystemService<InputMethodManager>() ?: return

		// The Fire OS on-screen keyboard blocks virtually all content on screen,
		// so require manually opening the keyboard there
		if (hasFocus && !DeviceUtils.isFireTv()) inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
		else if (!hasFocus) inputMethodManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
	}
}
