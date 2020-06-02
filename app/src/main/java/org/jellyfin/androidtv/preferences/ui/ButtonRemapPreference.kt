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

	init {
		// Explicitly set the layout or it will crash
		dialogLayoutResource = R.layout.button_remap_preference
		summaryProvider = ButtonRemapSummaryProvider.instance
	}

	/**
	 * Saves a KeyCode in this preference.
	 *
	 * @param keyCode the KeyCode to save
	 */
	fun setKeyCode(keyCode: Int) {
		persistInt(keyCode)
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

	override fun onGetDefaultValue(styledAttributes: TypedArray, index: Int): Any {
		return styledAttributes.getInt(index, -1)
	}

	internal class ButtonRemapSummaryProvider private constructor() : SummaryProvider<ButtonRemapPreference> {
		override fun provideSummary(preference: ButtonRemapPreference): CharSequence {
			return provideSummary(preference.context, preference.getKeyCode())
		}

		fun provideSummary(context: Context, keyCode: Int): CharSequence {
			val keyCodeString = KeyEvent.keyCodeToString(keyCode)
			return if (keyCodeString.startsWith("KEYCODE")) {
				keyCodeString.removePrefix("KEYCODE_").toLowerCase(Locale.getDefault()).split("_").joinToString(" ") { it.capitalize() }
			} else {
				context.getString(R.string.lbl_unknown_key, keyCodeString)
			}
		}

		companion object {
			val instance by lazy { ButtonRemapSummaryProvider() }
		}
	}
}

