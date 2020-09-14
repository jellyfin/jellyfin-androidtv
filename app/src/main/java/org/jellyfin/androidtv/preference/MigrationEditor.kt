package org.jellyfin.androidtv.preference

import android.content.SharedPreferences

typealias MigrationEditor = SharedPreferences.Editor

fun <T : Enum<T>> MigrationEditor.putEnum(key: String, value: T) {
	putString(key, value.toString())
}
