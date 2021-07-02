package org.jellyfin.androidtv.preference

/**
 * Interface defining the required functions for a preference store.
 */
interface PreferenceStore {
	// val value = store[Preference.x]
	operator fun <T : Preference<V>, V : Any> get(preference: T): V
	operator fun <T : Preference<V>, V : Enum<V>> get(preference: T): V

	// store[Preference.x] = value
	operator fun <T : Preference<V>, V : Any> set(preference: T, value: V)
	operator fun <T : Preference<V>, V : Enum<V>> set(preference: T, value: V)

	// store.getDefaultValue(Preference.x)
	fun <T : Preference<V>, V : Any> getDefaultValue(preference: T): V {
		return preference.defaultValue
	}

	// store.reset(Preference.x)
	fun <T : Preference<V>, V : Any> reset(preference: T) {
		this[preference] = getDefaultValue(preference)
	}

	// store.delete(Preference.x)
	fun <T : Preference<V>, V : Any> delete(preference: T)
}
