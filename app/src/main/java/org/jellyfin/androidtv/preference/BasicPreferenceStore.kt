package org.jellyfin.androidtv.preference

abstract class BasicPreferenceStore : IPreferenceStore {

	override fun <T : Any> getDefaultValue(preference: Preference<T>): T {
		return preference.defaultValue.data
	}

	override fun <T : Any> reset(preference: Preference<T>) {
		this[preference] = getDefaultValue(preference)
	}

	@Suppress("UNCHECKED_CAST")
	override operator fun <T : Any> get(preference: Preference<T>): T =
		// Coerce returned type based on the default type
		when (preference.defaultValue) {
			is PreferenceVal.IntT -> getInt(preference.key, preference.defaultValue.data)
			is PreferenceVal.LongT -> getLong(preference.key, preference.defaultValue.data)
			is PreferenceVal.BoolT -> getBool(preference.key, preference.defaultValue.data)
			is PreferenceVal.StringT -> getString(preference.key, preference.defaultValue.data)
			is PreferenceVal.EnumT -> getEnum(preference, preference.defaultValue)
		} as T

	override operator fun <T : Any> set(preference: Preference<T>, value: T) {
		when (value) {
			is Boolean -> setBool(preference.key, value)
			is Int -> setInt(preference.key, value)
			is Long -> setLong(preference.key, value)
			is String -> setString(preference.key, value)
			is Enum<*> -> setEnum(preference.key, value)
			else -> throw UnsupportedPreferenceType("Unknown Type")
		}
	}

	fun <T : Enum<T>> buildBasedOnT(value: T): PreferenceVal<T> = PreferenceVal.EnumT(value)

	// Protected methods to get / set items, this is an implementation detail so we protect
	// it in the abstract common functionality (where it is used)
	protected abstract fun getInt(keyName: String, defaultValue: Int): Int
	protected abstract fun getLong(keyName: String, defaultValue: Long): Long
	protected abstract fun getBool(keyName: String, defaultValue: Boolean): Boolean
	protected abstract fun getString(keyName: String, defaultValue: String): String

	protected abstract fun setInt(keyName: String, value: Int)
	protected abstract fun setLong(keyName: String, value: Long)
	protected abstract fun setBool(keyName: String, value: Boolean)
	protected abstract fun setString(keyName: String, value: String)

	// Private Enum handling, all Enum types are serialized to / from String types
	private fun <T> getEnum(
		preference: Preference<*>,
		// Require an EnumT param so someone can't call this with the wrong T type
		defaultValue: PreferenceVal.EnumT<*>
	): T {
		val stringValue = getString(preference.key, "")

		if (stringValue.isBlank()) {
			@Suppress("UNCHECKED_CAST")
			return defaultValue.data as T
		}

		val loadedVal = defaultValue.enumClass.java.enumConstants?.find {
			(it is PreferenceEnum && it.serializedName == stringValue) || it.name == stringValue
		} ?: defaultValue.data

		@Suppress("UNCHECKED_CAST")
		return loadedVal as T
	}

	private fun <V : Enum<V>> setEnum(keyName: String, value: Enum<V>) =
		setString(
			keyName, when (value) {
				is PreferenceEnum -> value.serializedName
				else -> value.toString()
			}
		)
}
