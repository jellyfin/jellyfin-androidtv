package org.jellyfin.androidtv.preference

/**
 * Interface defining the required functions for a preference store.
 */
interface PreferenceStore {
	// val value = store[Preference.x]
	@Suppress("UNCHECKED_CAST")
	operator fun <T : Any> get(preference: Preference<T>): T =
		// Coerce returned type based on the default type
		when (preference.defaultValue) {
			is Int -> getInt(preference.key, preference.defaultValue)
			is Long -> getLong(preference.key, preference.defaultValue)
			is Boolean -> getBool(preference.key, preference.defaultValue)
			is String -> getString(preference.key, preference.defaultValue)
			else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
		} as T

	// val value = store[Preference.x]
	override operator fun <T : Any> set(preference: Preference<T>, value: T) =
		when (value) {
			is Int -> setInt(preference.key, value)
			is Long -> setLong(preference.key, value)
			is Boolean -> setBool(preference.key, value)
			is String -> setString(preference.key, value)
			is Enum<*> -> setEnum(preference, value)
			else -> throw IllegalArgumentException("${preference.type.simpleName} type is not supported")
		}
	
	// store.getDefaultValue(Preference.x)
	fun <T : Any> getDefaultValue(preference: Preference<T>): T {
		return preference.defaultValue
	}

	// store.reset(Preference.x)
	fun <T : Any> reset(preference: Preference<T>) {
		this[preference] = getDefaultValue(preference)
	}

	// store.delete(Preference.x)
	fun <T : Preference<V>, V : Any> delete(preference: T)

	// Methods to get / set items, this is an implementation detail so we should mark
	// them protected in deriving classes and force callers to use get/set from above
	fun getInt(keyName: String, defaultValue: Int): Int
	fun getLong(keyName: String, defaultValue: Long): Long
	fun getBool(keyName: String, defaultValue: Boolean): Boolean
	fun getString(keyName: String, defaultValue: String): String

	fun setInt(keyName: String, value: Int)
	fun setLong(keyName: String, value: Long)
	fun setBool(keyName: String, value: Boolean)
	fun setString(keyName: String, value: String)


}
