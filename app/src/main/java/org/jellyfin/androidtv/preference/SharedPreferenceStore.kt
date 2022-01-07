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
	override operator fun <T : Any> get(preference: Preference<T>): T =
		// Coerce returned type based on the default type
		when (preference.defaultValue) {
			is PreferenceVal.IntT -> sharedPreferences.getInt(
				preference.key,
				preference.defaultValue.data
			)

			is PreferenceVal.LongT ->
				sharedPreferences.getLong(
					preference.key,
					preference.defaultValue.data

				)
			is PreferenceVal.BoolT ->
				sharedPreferences.getBoolean(
					preference.key,
					preference.defaultValue.data
				)

			is PreferenceVal.StringT -> {

				sharedPreferences.getString(
					preference.key,
					preference.defaultValue.data
				) ?: preference.defaultValue.data

			}
			is PreferenceVal.EnumT -> {
				getEnum(preference, preference.defaultValue)
			}
		} as T


	// Enums
	private fun <T> getEnum(
		preference: Preference<*>,
		// Require an EnumT param so someone can't call this with the wrong T type
		defaultValue: PreferenceVal.EnumT<*>
	): T {
		val stringValue = sharedPreferences.getString(preference.key, null)

		if (stringValue.isNullOrBlank()) {
			@Suppress("UNCHECKED_CAST")
			return defaultValue.data as T
		}

		val loadedVal = defaultValue.enumClass.java.enumConstants?.find {
			(it is PreferenceEnum && it.serializedName == stringValue) || it.name == stringValue
		} ?: defaultValue.data

		@Suppress("UNCHECKED_CAST")
		return loadedVal as T
	}

	override operator fun set(preference: Preference<*>, value: PreferenceVal<*>) =
		transaction {
			when (value) {
				is PreferenceVal.IntT -> putInt(preference.key, value.data)
				is PreferenceVal.LongT -> putLong(preference.key, value.data)
				is PreferenceVal.BoolT -> putBoolean(preference.key, value.data)
				is PreferenceVal.StringT -> putString(preference.key, value.data)
				is PreferenceVal.EnumT<*> -> setEnum(preference, value.data)
			}
		}


	private fun <V : Enum<V>> setEnum(preference: Preference<*>, value: Enum<V>) = transaction {
		putString(
			preference.key, when (value) {
				is PreferenceEnum -> value.serializedName
				else -> value.toString()
			}
		)
	}

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
