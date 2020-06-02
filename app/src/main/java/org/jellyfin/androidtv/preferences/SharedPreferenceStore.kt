package org.jellyfin.androidtv.preferences

import android.content.SharedPreferences
import timber.log.Timber

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
	// Internal helpers
	private fun transaction(body: SharedPreferences.Editor.() -> Unit) {
		val editor = sharedPreferences.edit()
		editor.body()
		editor.apply()
	}

	// Getters and setters
	// Primitive types
	@Suppress("UNCHECKED_CAST")
	operator fun <T : Preference<V>, V : Any> get(preference: T): V {
		return when (preference.type) {
			Int::class -> sharedPreferences.getInt(preference.key, preference.defaultValue as Int) as V
			Long::class -> sharedPreferences.getLong(preference.key, preference.defaultValue as Long) as V
			Boolean::class -> sharedPreferences.getBoolean(preference.key, preference.defaultValue as Boolean) as V
			String::class -> sharedPreferences.getString(preference.key, preference.defaultValue as String) as V

			else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
		}
	}

	operator fun <T : Preference<V>, V : Any> set(preference: T, value: V) = transaction {
		when (preference.type) {
			Int::class -> putInt(preference.key, value as Int)
			Long::class -> putLong(preference.key, value as Long)
			Boolean::class -> putBoolean(preference.key, value as Boolean)
			String::class -> putString(preference.key, value as String)
			Enum::class -> putString(preference.key, value.toString())

			else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
		}
	}

	// Enums
	operator fun <T : Preference<V>, V: Enum<V>> get(preference: T): V {
		val stringValue = sharedPreferences.getString(preference.key, null)

		return if (stringValue == null) preference.defaultValue
		else preference.type.java.enumConstants?.find { it.name == stringValue } ?: preference.defaultValue
	}

	operator fun <T : Preference<V>, V: Enum<V>> set(preference: T, value: V) = transaction {
		putString(preference.key, value.toString())
	}

	// Additional mutations
	fun <T : Preference<V>, V : Any> getDefaultValue(preference: T): V {
		return preference.defaultValue
	}

	fun <T : Preference<V>, V : Any> reset(preference: T) {
		this[preference] = getDefaultValue(preference)
	}

	fun <T : Preference<V>, V : Any> delete(preference: T) = transaction {
		remove(preference.key)
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
		val currentVersion = this[VERSION]
		if (currentVersion < toVersion) {
			Timber.i("Migrating a preference store from version $currentVersion to $toVersion")

			// Create a new editor and execute the migration
			transaction {
				body(sharedPreferences)
			}

			// Update current store version
			this[VERSION] = toVersion
		}
	}

	companion object {
		/**
		 * Version of the preference store. Used for migration.
		 */
		val VERSION = Preference.int("store_version", 1)
	}
}
