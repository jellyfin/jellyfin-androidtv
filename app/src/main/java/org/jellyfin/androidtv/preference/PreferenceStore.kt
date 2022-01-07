package org.jellyfin.androidtv.preference

/**
 * Interface defining the required functions for a preference store.
 */
interface PreferenceStore {
	// val value = store[Preference.x]
	// Preserve the type so downstream callers do not need to (re)cast
	operator fun <T : Any> get(preference: Preference<T>): T

	// store[Preference.x] = value
	operator fun set(preference: Preference<*>, value: PreferenceVal<*>)

	// store.getDefaultValue(Preference.x)
	fun <T : Any> getDefaultValue(preference: Preference<T>): T {
		return preference.defaultValue.data
	}

	// store.reset(Preference.x)
	fun <T : Any> reset(preference: Preference<T>) {
		this[preference] = PreferenceVal.buildBasedOnT(getDefaultValue(preference))
	}

	// store.delete(Preference.x)
	fun delete(preference: Preference<*>)
}
