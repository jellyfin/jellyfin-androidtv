package org.jellyfin.preference.store

import android.content.SharedPreferences
import org.jellyfin.preference.Preference
import org.jellyfin.preference.PreferenceEnum
import org.jellyfin.preference.intPreference
import org.jellyfin.preference.migration.MigrationContext
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
@Suppress("TooManyFunctions")
abstract class SharedPreferenceStore(
	/**
	 * SharedPreferences to read from and write to
	 */
	protected val sharedPreferences: SharedPreferences
) : PreferenceStore<SharedPreferences.Editor, SharedPreferences>() {
	// Internal helpers
	private fun transaction(body: SharedPreferences.Editor.() -> Unit) {
		val editor = sharedPreferences.edit()
		editor.body()
		editor.apply()
	}

	override fun getInt(key: String, defaultValue: Int) =
		sharedPreferences.getInt(key, defaultValue)

	override fun getLong(key: String, defaultValue: Long) =
		sharedPreferences.getLong(key, defaultValue)

	override fun getFloat(key: String, defaultValue: Float) =
		sharedPreferences.getFloat(key, defaultValue)

	override fun getBool(key: String, defaultValue: Boolean) =
		sharedPreferences.getBoolean(key, defaultValue)

	override fun getString(key: String, defaultValue: String) =
		sharedPreferences.getString(key, defaultValue) ?: defaultValue

	override fun setInt(key: String, value: Int) = transaction { putInt(key, value) }
	override fun setLong(key: String, value: Long) = transaction { putLong(key, value) }
	override fun setFloat(key: String, value: Float) = transaction { putFloat(key, value) }
	override fun setBool(key: String, value: Boolean) =
		transaction { putBoolean(key, value) }

	override fun setString(key: String, value: String) =
		transaction { putString(key, value) }

	override fun <T : Enum<T>> getEnum(preference: Preference<T>): T {
		val stringValue = getString(preference.key, "")
		return if (stringValue.isBlank()) preference.defaultValue
		else preference.type.java.enumConstants?.find {
			(it is PreferenceEnum && it.serializedName == stringValue) || it.name == stringValue
		} ?: preference.defaultValue
	}

	override fun <V : Enum<V>> setEnum(preference: Preference<*>, value: Enum<V>) =
		setString(preference.key, when (value) {
				is PreferenceEnum -> value.serializedName ?: value.name
				else -> value.name
			})

	// Additional mutations
	override fun <T : Any> delete(preference: Preference<T>) = transaction {
		remove(preference.key)
	}

	// Migrations
	override fun runMigrations(body: MigrationContext<SharedPreferences.Editor, SharedPreferences>.() -> Unit) {
		val context = MigrationContext<SharedPreferences.Editor, SharedPreferences>()
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
		val VERSION = intPreference("store_version", -1)
	}
}
