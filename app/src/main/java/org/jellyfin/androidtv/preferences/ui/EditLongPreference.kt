package org.jellyfin.androidtv.preferences.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import timber.log.Timber

class EditLongPreference(context: Context, attrs: AttributeSet?) : EditTextPreference(context, attrs) {
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
