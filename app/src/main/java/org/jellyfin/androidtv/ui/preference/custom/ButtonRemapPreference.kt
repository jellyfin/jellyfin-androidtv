package org.jellyfin.androidtv.ui.preference.custom

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.preference.DialogPreference
import org.jellyfin.androidtv.R
import java.util.Locale

class ButtonRemapPreference @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
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
					.lowercase(Locale.getDefault())
					.split("_")
					.joinToString(" ") {
						it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
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

