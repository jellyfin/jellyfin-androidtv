package org.jellyfin.androidtv.ui.shared

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import org.jellyfin.androidtv.util.DeviceUtils

class KeyboardFocusChangeListener : View.OnFocusChangeListener {
	override fun onFocusChange(view: View, hasFocus: Boolean) {
		// The Fire OS on-screen keyboard blocks virtually all content on screen,
		// so require manually opening the keyboard there
		if (DeviceUtils.isFireTv()) return

		view.context.getSystemService<InputMethodManager>()?.apply {
			if (!hasFocus) hideSoftInputFromWindow(view.windowToken, 0)
			else showSoftInput(view, 0)
		}
	}
}
