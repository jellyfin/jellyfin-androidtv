package org.jellyfin.androidtv.preferences.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import org.jellyfin.androidtv.R

class ButtonRemapPreference(
	context: Context,
	attrs: AttributeSet? = null
) : DialogPreference(context, attrs) {

	/**
	 * Saves a KeyCode in this preference.
	 *
	 * @param mKeyCode the KeyCode to save
	 */
	fun setKeyCode(mKeyCode: Int) {
		persistInt(mKeyCode)
	}

	/**
	 * Returns the saved KeyCode preference.
	 *
	 * @return the saved KeyCode
	 */
	fun getKeyCode(): Int {
		return getPersistedInt(-1)
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		if (defaultValue != null)
			persistInt(defaultValue as Int)
	}

	override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
		return a!!.getInt(index, -1)
	}

	init {
		// Explicitly set the layout or it will crash
		dialogLayoutResource = R.layout.button_remap_preference
	}

}
