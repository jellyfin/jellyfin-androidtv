package org.jellyfin.androidtv.ui.shared

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import org.jellyfin.androidtv.util.DeviceUtils

class KeyboardFocusChangeListener : View.OnFocusChangeListener {
	override fun onFocusChange(view: View, hasFocus: Boolean) {
		view.context.getSystemService<InputMethodManager>()?.apply {
			if (!hasFocus) hideSoftInputFromWindow(view.windowToken, 0)
			// The Fire OS on-screen keyboard blocks virtually all content on screen,
			// so require manually opening the keyboard there
			else if (!DeviceUtils.isFireTv()) showSoftInput(view, 0)
		}
	}
}
