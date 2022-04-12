package org.jellyfin.preference.migration

import android.content.SharedPreferences

fun <T : Enum<T>> SharedPreferences.Editor.putEnum(key: String, value: T) {
	putString(key, value.toString())
}
