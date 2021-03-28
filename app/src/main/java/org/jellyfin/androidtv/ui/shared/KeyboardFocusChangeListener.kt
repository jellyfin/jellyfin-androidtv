package org.jellyfin.androidtv.ui.shared

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService

class KeyboardFocusChangeListener : View.OnFocusChangeListener {
	override fun onFocusChange(view: View, hasFocus: Boolean) {
		view.context.getSystemService<InputMethodManager>()?.apply {
			if (!hasFocus) hideSoftInputFromWindow(view.windowToken, 0)
			else showSoftInput(view, 0)
		}
	}
}
