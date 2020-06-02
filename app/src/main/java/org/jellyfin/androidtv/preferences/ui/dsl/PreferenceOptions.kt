package org.jellyfin.androidtv.preferences.ui.dsl

import org.jellyfin.androidtv.preferences.Preference
import org.jellyfin.androidtv.preferences.SharedPreferenceStore

class PreferenceOptions<T>(
	val get: () -> T,
	val set: (value: T) -> Unit,
	val enabled: (() -> Boolean),
	val visible: (() -> Boolean)
) {
	class Builder<T> {
		private var getter: (() -> T)? = null
		private var setter: ((value: T) -> Unit)? = null
		private var enabled: (() -> Boolean)? = null
		private var visible: (() -> Boolean)? = null

		fun get(getter: () -> T) {
			this.getter = getter
		}

		fun set(setter: (value: T) -> Unit) {
			this.setter = setter
		}

		fun enabled(enabled: () -> Boolean) {
			this.enabled = enabled
		}

		fun visible(visible: () -> Boolean) {
			this.visible = visible
		}

		fun build(): PreferenceOptions<T> {
			if (getter == null) throw NullPointerException("getter was null")
			if (setter == null) throw NullPointerException("setter was null")

			return PreferenceOptions(
				getter!!,
				setter!!,
				enabled ?: { true },
				visible ?: { true }
			)
		}
	}
}

// Bind enums
// Implementation is the same but the compiler requires the type information
fun <T : Enum<T>> PreferenceOptions.Builder<T>.bindEnum(
	store: SharedPreferenceStore,
	preference: Preference<T>
) {
	get { store[preference] }
	set { store[preference] = it }
}

// Bind primitive types
fun <T : Any> PreferenceOptions.Builder<T>.bind(
	store: SharedPreferenceStore,
	preference: Preference<T>
) {
	get { store[preference] }
	set { store[preference] = it }
}
