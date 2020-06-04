package org.jellyfin.androidtv.preferences.ui.preference

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.preference.DialogPreference
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.Preference
import org.jellyfin.androidtv.preferences.SharedPreferenceStore
import java.util.*

class ButtonRemapPreference(
	context: Context,
	attrs: AttributeSet? = null
) : DialogPreference(context, attrs) {
	override fun getDialogLayoutResource() = R.layout.button_remap_preference

	private var store: SharedPreferenceStore? = null
	private var preference: Preference<Int>? = null

	fun setPreference(store: SharedPreferenceStore, preference: Preference<Int>) {
		this.store = store
		this.preference = preference

		notifyChanged()
	}

	var keyCode: Int
		get() {
			return store!![preference!!]
		}
		set(value) {
			store!![preference!!] = value
		}

	val defaultKeyCode: Int
		get() = store!!.getDefaultValue(preference!!)

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

