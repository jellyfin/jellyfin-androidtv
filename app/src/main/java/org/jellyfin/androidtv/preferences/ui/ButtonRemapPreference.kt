package org.jellyfin.androidtv.preferences.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.preference.DialogPreference
import androidx.preference.Preference.SummaryProvider
import org.jellyfin.androidtv.R
import java.util.*

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

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		return a.getInt(index, -1)
	}

	init {
		// Explicitly set the layout or it will crash
		dialogLayoutResource = R.layout.button_remap_preference
		summaryProvider = ButtonRemapSummaryProvider.instance
	}

	class ButtonRemapSummaryProvider private constructor() : SummaryProvider<ButtonRemapPreference> {
		override fun provideSummary(preference: ButtonRemapPreference): CharSequence {
			return provideSummary(preference.context, preference.getKeyCode())
		}

		fun provideSummary(context: Context, keyCode: Int): CharSequence {
			var keyCodeString = KeyEvent.keyCodeToString(keyCode)
			if (keyCodeString.startsWith("KEYCODE")) {
				keyCodeString = keyCodeString.split("_").drop(1).joinToString(" ") { e -> e.toLowerCase(Locale.getDefault()).capitalize() }
			} else {
				keyCodeString = "${context.getString(R.string.lbl_unknown_key)} ($keyCodeString)"
			}
			return keyCodeString
		}

		companion object {
			val instance by lazy { ButtonRemapSummaryProvider() }
		}
	}
}


