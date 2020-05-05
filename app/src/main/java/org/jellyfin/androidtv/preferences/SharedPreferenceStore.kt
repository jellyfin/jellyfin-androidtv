package org.jellyfin.androidtv.preferences

import android.content.SharedPreferences
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Basis for preference stores. Provides functions for delegated properties and migration functionality.
 *
 * Preferences are added as properties and look like this:
 * ```kotlin
 * 	var example by stringPreference("example", "default")
 * 	```
 * Specify as "val" instead of "var" to make it read-only.
 *
 * Migrations should be added to the `init` block of a store and look like this:
 * ```kotlin
 * migration(toVersion = 1) {
 * 	// Get a value
 * 	it.getString("example", "default")
 * 	// Set a value
 * 	setString("example", "new value")
 * }
 * ```
 */
abstract class SharedPreferenceStore(
	/**
	 * SharedPreferences to read from and write to
	 */
	protected val sharedPreferences: SharedPreferences
) {
	/**
	 * Version of the preference store. Used for migration.
	 */
	var version by intPreference("store_version", 1)
		private set

	// Basic types
	/**
	 * Delegated property function for integers
	 *
	 * @param key Key used to store setting as
	 * @param default Default value
	 *
	 * @return Delegated property
	 */
	protected fun intPreference(key: String, default: Int) = object : ReadWriteProperty<SharedPreferenceStore, Int> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): Int {
			return sharedPreferences.getInt(key, default)
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: Int) {
			sharedPreferences.edit().putInt(key, value).apply()
		}
	}
	
	/**
	 * Delegated property function for longs
	 *
	 * @param key Key used to store setting as
	 * @param default Default value
	 *
	 * @return Delegated property
	 */
	protected fun longPreference(key: String, default: Long) = object : ReadWriteProperty<SharedPreferenceStore, Long> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): Long {
			return sharedPreferences.getLong(key, default)
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: Long) {
			sharedPreferences.edit().putLong(key, value).apply()
		}
	}

	/**
	 * Delegated property function for booleans
	 *
	 * @param key Key used to store setting as
	 * @param default Default value
	 *
	 * @return Delegated property
	 */
	protected fun booleanPreference(key: String, default: Boolean) = object : ReadWriteProperty<SharedPreferenceStore, Boolean> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): Boolean {
			return sharedPreferences.getBoolean(key, default)
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: Boolean) {
			sharedPreferences.edit().putBoolean(key, value).apply()
		}
	}

	/**
	 * Delegated property function for strings
	 *
	 * @param key Key used to store setting as
	 * @param default Default value
	 *
	 * @return Delegated property
	 */
	protected fun stringPreference(key: String, default: String) = object : ReadWriteProperty<SharedPreferenceStore, String> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): String {
			return sharedPreferences.getString(key, null) ?: default
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: String) {
			sharedPreferences.edit().putString(key, value).apply()
		}
	}

	/**
	 * Delegated property function for nullable strings
	 *
	 * @param key Key used to store setting as
	 * @param default Default value
	 *
	 * @return Delegated property
	 */
	protected fun stringPreferenceNullable(key: String, default: String?) = object : ReadWriteProperty<SharedPreferenceStore, String?> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): String? {
			return sharedPreferences.getString(key, null) ?: default
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: String?) {
			if (value == null) sharedPreferences.edit().remove(key).apply()
			else sharedPreferences.edit().putString(key, value).apply()
		}
	}

	// Custom types
	/**
	 * Delegated property function for enums. Uses strings internally
	 *
	 * @param T Enum class for allowed values
	 * @param key Key used to store setting as
	 * @param default Default value
	 *
	 * @return Delegated property
	 */
	protected inline fun <reified T : Enum<T>> enumPreference(key: String, default: T) = object : ReadWriteProperty<SharedPreferenceStore, T> {
		override fun getValue(thisRef: SharedPreferenceStore, property: KProperty<*>): T {
			val stringValue = sharedPreferences.getString(key, null)

			return if (stringValue == null) default
			else T::class.java.enumConstants?.find { it.name == stringValue } ?: default
		}

		override fun setValue(thisRef: SharedPreferenceStore, property: KProperty<*>, value: T) {
			sharedPreferences.edit().putString(key, value.toString()).apply()
		}
	}

	// Migrations
	/**
	 * Migration function to upgrade the preferences in older app versions
	 *
	 * Migrations should be added to the `init` block of a store and look like this:
	 * ```kotlin
	 * migration(toVersion = 1) {
	 * 	// Get a value
	 * 	it.getString("example", "default")
	 * 	// Set a value
	 * 	setString("example", "new value")
	 * }
	 *
	 * @param toVersion The new version to upgrade to
	 * @param body Actual migrationb code
	 */
	protected fun migration(toVersion: Int, body: MigrationEditor.(SharedPreferences) -> Unit) {
		// Check if migration should be performed
		if (version < toVersion) {
			Timber.i("Migrating a preference store from version $version to $toVersion")

			// Create a new editor and execute the migration
			val editor = sharedPreferences.edit()
			body(editor, sharedPreferences)
			editor.apply()

			// Update current store version
			version = toVersion
		}
	}
}
