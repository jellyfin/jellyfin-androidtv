package org.jellyfin.androidtv.preferences.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceViewHolder

class EditLongPreference : EditTextPreference {
	constructor(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attributeSet, defStyleAttr, defStyleRes)
	constructor(context: Context?, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)
	constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet)
	constructor(context: Context?) : super(context)

	override fun getPersistedString(defaultReturnValue: String?): String {
		return getPersistedLong(-1).toString()
	}

	override fun persistString(value: String?): Boolean {
		return try {
			persistLong(value!!.toLong())
		} catch (e: NumberFormatException) {
			false
		}
	}
}
