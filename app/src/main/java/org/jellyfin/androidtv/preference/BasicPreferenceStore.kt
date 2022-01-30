package org.jellyfin.androidtv.preference

abstract class BasicPreferenceStore : PreferenceStore {

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

	private fun <T : Enum<T>> getEnum(preference: Preference<T>): T {
		val stringValue = getString(preference.key, "")

		if (stringValue.isBlank()) {
			return preference.defaultValue
		}

		val loadedVal = preference.type.java.enumConstants?.find {
			(it is PreferenceEnum && it.serializedName == stringValue) || it.name == stringValue
		} ?: preference.defaultValue
		return loadedVal
	}

	private fun <V : Enum<V>> setEnum(preference: Preference<*>, value: Enum<V>) =
		setString(
			preference.key, when (value) {
				is PreferenceEnum -> value.serializedName
				else -> value.toString()
			}
		)
}
