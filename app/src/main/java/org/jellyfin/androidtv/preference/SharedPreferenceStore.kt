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
) : IPreferenceStore, BasicPreferenceStore() {
	// Internal helpers
	private fun transaction(body: SharedPreferences.Editor.() -> Unit) {
		val editor = sharedPreferences.edit()
		editor.body()
		editor.apply()
	}

	override fun getInt(keyName: String, defaultValue: Int) =
		sharedPreferences.getInt(keyName, defaultValue)

	override fun getLong(keyName: String, defaultValue: Long) =
		sharedPreferences.getLong(keyName, defaultValue)

	override fun getBool(keyName: String, defaultValue: Boolean) =
		sharedPreferences.getBoolean(keyName, defaultValue)

	override fun getString(keyName: String, defaultValue: String) =
		sharedPreferences.getString(keyName, defaultValue) ?: defaultValue

	override fun setInt(keyName: String, value: Int) = transaction { putInt(keyName, value) }
	override fun setLong(keyName: String, value: Long) = transaction { putLong(keyName, value) }
	override fun setBool(keyName: String, value: Boolean) =
		transaction { putBoolean(keyName, value) }

	override fun setString(keyName: String, value: String) =
		transaction { putString(keyName, value) }

	// Additional mutations
	override fun delete(preference: Preference<*>) = transaction {
		remove(preference.key)
	}

	// Migrations
	protected fun runMigrations(body: MigrationContext<MigrationEditor, SharedPreferences>.() -> Unit) {
		val context = MigrationContext<MigrationEditor, SharedPreferences>()
		context.body()

		val newVersion = context.applyMigrations(this[VERSION]) { migration ->
			Timber.i("Migrating a preference store to version ${migration.toVersion}")

			// Create a new transaction and execute the migration
			transaction { migration.body(this, sharedPreferences) }
		}
		this[VERSION] = PreferenceVal.IntT(newVersion)
	}

	companion object {
		/**
		 * Version of the preference store. Used for migration.
		 */
		val VERSION = Preference.int("store_version", -1)
	}
}
