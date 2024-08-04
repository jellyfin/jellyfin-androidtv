package org.jellyfin.preference.store

import org.jellyfin.preference.Preference
import org.jellyfin.preference.migration.MigrationContext

/**
 * Abstract class defining the required functions for a preference store.
 *
 * This implements shared functionality (such as Enum handling), whilst allowing for
 * different backing stores.
 */
@Suppress("TooManyFunctions")
abstract class PreferenceStore<ME, MV> {
	// val value = store[Preference.x]
	@Suppress("UNCHECKED_CAST")
	operator fun <T : Any> get(preference: Preference<T>): T =
		// Coerce returned type based on the default type
		when (preference.defaultValue) {
			is Int -> getInt(preference.key, preference.defaultValue)
			is Long -> getLong(preference.key, preference.defaultValue)
			is Float -> getFloat(preference.key, preference.defaultValue)
			is Boolean -> getBool(preference.key, preference.defaultValue)
			is String -> getString(preference.key, preference.defaultValue)
			else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
		} as T

	// val value = store[Preference.x]
	operator fun <T : Enum<T>> get(preference: Preference<T>) = getEnum(preference)

	// store[Preference.x] = value
	operator fun <T : Any> set(preference: Preference<T>, value: T) =
		when (value) {
			is Int -> setInt(preference.key, value)
			is Long -> setLong(preference.key, value)
			is Float -> setFloat(preference.key, value)
			is Boolean -> setBool(preference.key, value)
			is String -> setString(preference.key, value)
			is Enum<*> -> setEnum(preference, value)
			else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
		}

	// store.getDefaultValue(Preference.x)
	fun <T : Any> getDefaultValue(preference: Preference<T>): T = preference.defaultValue

	// store.reset(Preference.x)
	fun <T : Any> reset(preference: Preference<T>) {
		this[preference] = getDefaultValue(preference)
	}

	// store.delete(Preference.x)
	abstract fun <T : Any> delete(preference: Preference<T>)

	// Protected methods to get / set items, this is an implementation detail so we protect
	// it in the abstract common functionality (where it is used)
	protected abstract fun getInt(key: String, defaultValue: Int): Int
	protected abstract fun getLong(key: String, defaultValue: Long): Long
	protected abstract fun getFloat(key: String, defaultValue: Float): Float
	protected abstract fun getBool(key: String, defaultValue: Boolean): Boolean
	protected abstract fun getString(key: String, defaultValue: String): String

	protected abstract fun setInt(key: String, value: Int)
	protected abstract fun setLong(key: String, value: Long)
	protected abstract fun setFloat(key: String, value: Float)
	protected abstract fun setBool(key: String, value: Boolean)
	protected abstract fun setString(key: String, value: String)

	// Private Enum handling, all Enum types are serialized to / from String types
	protected abstract fun <T : Enum<T>> getEnum(preference: Preference<T>): T

	protected abstract fun <V : Enum<V>> setEnum(preference: Preference<*>, value: Enum<V>)

	// Migrations
	protected abstract fun runMigrations(body: MigrationContext<ME, MV>.() -> Unit)
}
