package org.jellyfin.androidtv.ui.navigation.focus

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusRequester

@Stable
class FocusPersistManager {
	private val requesters = mutableMapOf<String, FocusRequester>()
	private var lastFocusedKey: String? = null

	fun register(key: String, requester: FocusRequester) {
		requesters[key] = requester
	}

	fun unregister(key: String) {
		requesters.remove(key)
	}

	fun onFocusChanged(key: String, isFocused: Boolean) {
		if (isFocused) lastFocusedKey = key
	}

	fun save(): String? = lastFocusedKey

	fun restore(key: String?) {
		if (key == null) return

		requesters[key]?.requestFocus()
	}
}

val LocalFocusPersistManager = compositionLocalOf<FocusPersistManager> {
	error("No FocusPersistManager provided")
}
