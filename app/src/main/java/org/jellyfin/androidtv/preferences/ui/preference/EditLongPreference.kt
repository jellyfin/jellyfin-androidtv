package org.jellyfin.androidtv.preferences.ui.preference

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import timber.log.Timber

class EditLongPreference(context: Context, attrs: AttributeSet?) : EditTextPreference(context, attrs) {
	var value: Long
		get() {
			return getPersistedLong(-1)
		}
		set(value) {
			persistLong(value)
		}

	init {
		setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER and InputType.TYPE_NUMBER_FLAG_SIGNED }
	}

	override fun getPersistedString(defaultReturnValue: String?): String {
		return getPersistedLong(-1).toString()
	}

	override fun persistString(value: String?): Boolean {
		return try {
			persistLong(value!!.toLong())
		} catch (e: NumberFormatException) {
			Timber.e(e)
			false
		}
	}
}
