package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

class KeyboardFocusChangeListener : View.OnFocusChangeListener {
	override fun onFocusChange(v : View?, hasFocus : Boolean) {
		if (v != null) {
			(v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
				if (!hasFocus) hideSoftInputFromWindow(v.windowToken, 0)
				else showSoftInput(v, 0)
			}
		}
	}
}
