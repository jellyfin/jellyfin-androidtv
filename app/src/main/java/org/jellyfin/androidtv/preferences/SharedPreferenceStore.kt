package org.jellyfin.androidtv.preferences

import android.content.SharedPreferences
import org.jellyfin.androidtv.TvApp
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class SharedPreferenceStore(
	protected val sharedPreferences: SharedPreferences
) {
	var version by intPreference("store_version", 1)
		private set

	// Basic types
	protected fun intPreference(key: String, default: Int) = object : ReadWriteProperty<SharedPreferenceStore, Int> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): Int {
			return sharedPreferences.getInt(key, default)
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: Int) {
			sharedPreferences.edit().putInt(key, value).apply()
		}
	}

	protected fun booleanPreference(key: String, default: Boolean) = object : ReadWriteProperty<SharedPreferenceStore, Boolean> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): Boolean {
			return sharedPreferences.getBoolean(key, default)
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: Boolean) {
			sharedPreferences.edit().putBoolean(key, value).apply()
		}
	}

	//todo not-null variant?
	protected fun stringPreference(key: String, default: String?) = object : ReadWriteProperty<SharedPreferenceStore, String?> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): String? {
			return sharedPreferences.getString(key, default)
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: String?) {
			if (value == null) sharedPreferences.edit().remove(key).apply()
			else sharedPreferences.edit().putString(key, value).apply()
		}
	}

	// Custom types
	protected inline fun <reified T : Enum<T>> enumPreference(key: String, default: T?) = object : ReadWriteProperty<SharedPreferenceStore, T?> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): T? {
			val stringValue = sharedPreferences.getString(key, null)

			return if (stringValue == null) default
			else T::class.java.enumConstants.find { it.name == stringValue }
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: T?) {
			if (value == null) sharedPreferences.edit().remove(key).apply()
			else sharedPreferences.edit().putString(key, value.toString()).apply()
		}
	}

	// Migrations
	protected fun migration(toVersion: Int, migration: SharedPreferences.Editor.(SharedPreferences) -> Unit) {
		if (version < toVersion) {
			TvApp.getApplication().logger.Info("Migrating a preference store from version $version to $toVersion")

			val editor = sharedPreferences.edit()
			migration(editor, sharedPreferences)
			editor.apply()

			version = toVersion
		}
	}
}
