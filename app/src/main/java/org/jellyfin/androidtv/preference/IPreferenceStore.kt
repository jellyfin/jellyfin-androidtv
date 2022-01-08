package org.jellyfin.androidtv.preference

/**
 * Interface defining the required functions for a preference store.
 */
interface IPreferenceStore {
	// val value = store[Preference.x]
	// Preserve the type so downstream callers do not need to (re)cast
	operator fun <T : Any> get(preference: Preference<T>): T

	// store[Preference.x] = value
	operator fun <T : Any> set(preference: Preference<T>, value: T)

	// store.getDefaultValue(Preference.x)
	fun <T : Any> getDefaultValue(preference: Preference<T>): T

	// store.reset(Preference.x)
	fun <T : Any> reset(preference: Preference<T>)

	// store.delete(Preference.x)
	fun delete(preference: Preference<*>)
}
