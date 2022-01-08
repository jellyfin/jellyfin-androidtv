package org.jellyfin.androidtv.preference

import kotlinx.coroutines.runBlocking

interface AsyncPreferenceStore : IPreferenceStore {
	val shouldUpdate: Boolean

	/**
	 * Save values to store.
	 */
	suspend fun commit(): Boolean

	/**
	 * Update values from store.
	 */
	suspend fun update(): Boolean

	/**
	 * Modify the preferences in store and [commit] afterwards. Automatically calls [update] if
	 * [shouldUpdate] is true. Use `this` keyword to access preferences.
	 *
	 * ```kotlin
	 * store.transaction {
	 * 	// get
	 * 	val value = this[Preference.x]
	 * 	// set
	 * 	this[Preference.x] = value
	 * 	// get default
	 * 	getDefaultValue(Preference.x)
	 * 	// set default
	 * 	reset(Preference.x)
	 * 	// delete
	 * 	delete(Preference.x)
	 * }
	 * ```
	 */
	suspend fun transaction(body: AsyncPreferenceStore.() -> Unit): Boolean {
		if (shouldUpdate) update()

		body()

		return commit()
	}

	/**
	 * Compatability with old Java classes.
	 */
	fun updateBlocking() = runBlocking { update() }

	/**
	 * Compatability with old Java classes.
	 */
	fun commitBlocking() = runBlocking { commit() }
}
