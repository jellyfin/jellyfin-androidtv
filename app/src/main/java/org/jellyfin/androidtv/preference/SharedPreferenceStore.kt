package org.jellyfin.androidtv.preference

import android.content.SharedPreferences
import org.jellyfin.androidtv.preference.migrations.MigrationContext
import timber.log.Timber

/**
 * Implementation of the [PreferenceStore] using Android shared preferences.
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
	protected val sharedPreferences: SharedPreferences,
) : PreferenceStore {
	// Internal helpers
	private fun transaction(body: SharedPreferences.Editor.() -> Unit) {
		val editor = sharedPreferences.edit()
		editor.body()
		editor.apply()
	}

	// Getters and setters
	// Primitive types
	@Suppress("UNCHECKED_CAST")
	override operator fun <T : Preference<V>, V : Any> get(preference: T) = when (preference.type) {
		Int::class -> sharedPreferences.getInt(preference.key, preference.defaultValue as Int)
		Long::class -> sharedPreferences.getLong(preference.key, preference.defaultValue as Long)
		Boolean::class -> sharedPreferences.getBoolean(preference.key, preference.defaultValue as Boolean)
		String::class -> sharedPreferences.getString(preference.key, preference.defaultValue as String)

		else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
	} as V

	override operator fun <T : Preference<V>, V : Any> set(preference: T, value: V) = transaction {
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
	override operator fun <T : Preference<V>, V : Enum<V>> get(preference: T): V {
		val stringValue = sharedPreferences.getString(preference.key, null)

		return if (stringValue == null) preference.defaultValue
		else preference.type.java.enumConstants?.find {
			(it is PreferenceEnum && it.serializedName == stringValue) || it.name == stringValue
		} ?: preference.defaultValue
	}

	override fun <T : Preference<V>, V : Enum<V>> set(preference: T, value: V) = transaction {
		putString(preference.key, when (value) {
			is PreferenceEnum -> value.serializedName
			else -> value.toString()
		})
	}

	// Additional mutations
	override fun <T : Preference<V>, V : Any> delete(preference: T) = transaction {
		remove(preference.key)
	}

	// Migrations
	protected fun runMigrations(body: MigrationContext<MigrationEditor, SharedPreferences>.() -> Unit) {
		val context = MigrationContext<MigrationEditor, SharedPreferences>()
		context.body()

		this[VERSION] = context.applyMigrations(this[VERSION]) { migration ->
			Timber.i("Migrating a preference store to version ${migration.toVersion}")

			// Create a new transaction and execute the migration
			transaction { migration.body(this, sharedPreferences) }
		}
	}

	companion object {
		/**
		 * Version of the preference store. Used for migration.
		 */
		val VERSION = Preference.int("store_version", -1)
	}
}
