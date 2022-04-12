package org.jellyfin.preference.store

abstract class AsyncPreferenceStore<ME, MV> : PreferenceStore<ME, MV>() {
	abstract val shouldUpdate: Boolean

	/**
	 * Save values to store.
	 */
	abstract suspend fun commit(): Boolean

	/**
	 * Update values from store.
	 */
	abstract suspend fun update(): Boolean

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
	suspend fun transaction(body: AsyncPreferenceStore<ME, MV>.() -> Unit): Boolean {
		if (shouldUpdate) update()

		body()

		return commit()
	}
}
