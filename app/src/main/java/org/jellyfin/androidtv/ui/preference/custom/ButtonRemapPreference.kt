package org.jellyfin.androidtv.ui.preference.custom

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.preference.DialogPreference
import org.jellyfin.androidtv.R
import java.util.Locale

class ButtonRemapPreference(
	context: Context,
	attrs: AttributeSet? = null
) : DialogPreference(context, attrs) {
	override fun getDialogLayoutResource() = R.layout.preference_button_remap

	private var innerKeyCode: Int = -1
	var keyCode: Int
		get() = innerKeyCode
		set(value) {
			innerKeyCode = value
			notifyDependencyChange(false)
			notifyChanged()
		}
	var defaultKeyCode: Int = -1

	class ButtonRemapSummaryProvider : SummaryProvider<ButtonRemapPreference> {
		override fun provideSummary(preference: ButtonRemapPreference): CharSequence {
			return getKeycodeName(preference.context, preference.keyCode)
		}

		fun getKeycodeName(context: Context, keyCode: Int): CharSequence {
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

