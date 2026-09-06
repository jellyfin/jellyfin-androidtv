package org.jellyfin.androidtv.ui.navigation.focus

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusRequester

@Stable
class FocusPersistManager {
	private val requesters = mutableMapOf<String, FocusRequester>()
	private var lastFocusedKey: String? = null
	private var initialFocusRequester: FocusRequester? = null
	private var initialFocusAllowed = true
	private var restoredFocusKey: String? = null

	fun register(key: String, requester: FocusRequester, initialFocus: Boolean) {
		val previousRequester = requesters.put(key, requester)
		if (initialFocusRequester === previousRequester) initialFocusRequester = null
		if (initialFocus && initialFocusAllowed) initialFocusRequester = requester
	}

	fun unregister(key: String) {
		val requester = requesters.remove(key)
		if (initialFocusRequester === requester) initialFocusRequester = null
	}

	fun onFocusChanged(key: String, isFocused: Boolean) {
		if (!isFocused) return

		lastFocusedKey = key
		if (initialFocusRequester == null || requesters[key] === initialFocusRequester) {
			initialFocusAllowed = false
		}
	}

	fun requestInitialFocus(requester: FocusRequester) {
		if (!initialFocusAllowed || restoredFocusKey != null || initialFocusRequester !== requester) return

		if (requester.requestFocus()) initialFocusAllowed = false
	}

	fun save(): String? = lastFocusedKey

	fun restore(key: String?) {
		restoredFocusKey = key
		if (key == null) return

		initialFocusAllowed = false
		requesters[key]?.requestFocus()
	}
}

val LocalFocusPersistManager = compositionLocalOf<FocusPersistManager> {
	error("No FocusPersistManager provided")
}
