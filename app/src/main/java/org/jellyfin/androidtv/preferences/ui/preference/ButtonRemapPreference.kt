package org.jellyfin.androidtv.preferences.ui.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.preference.DialogPreference
import org.jellyfin.androidtv.R
import java.util.*

class ButtonRemapPreference(
	context: Context,
	attrs: AttributeSet? = null
) : DialogPreference(context) {
	override fun getDialogLayoutResource() = R.layout.button_remap_preference

	var keyCode: Int = -1
		private set

	/**
	 * Saves a KeyCode in this preference.
	 *
	 * @param keyCode the KeyCode to save
	 */
	fun setKeyCode(keyCode: Int) {
		if (keyCode == this.keyCode) return

		this.keyCode = keyCode
		persistInt(keyCode)
		notifyChanged()
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		if (defaultValue is Int)
			setKeyCode(defaultValue)
	}

	override fun onGetDefaultValue(styledAttributes: TypedArray, index: Int): Any {
		return styledAttributes.getInt(index, -1)
	}

	class ButtonRemapSummaryProvider : SummaryProvider<ButtonRemapPreference> {
		override fun provideSummary(preference: ButtonRemapPreference): CharSequence {
			return provideSummary(preference.context, preference.keyCode)
		}

		fun provideSummary(context: Context, keyCode: Int): CharSequence {
			val keyCodeString = KeyEvent.keyCodeToString(keyCode)

			return if (keyCodeString.startsWith("KEYCODE")) {
				keyCodeString
					.removePrefix("KEYCODE_")
					.toLowerCase(Locale.getDefault())
					.split("_")
					.joinToString(" ") {
						it.capitalize()
					}
			} else {
				context.getString(R.string.lbl_unknown_key, keyCodeString)
			}
		}

		companion object {
			val instance by lazy { ButtonRemapSummaryProvider() }
		}
	}
}

