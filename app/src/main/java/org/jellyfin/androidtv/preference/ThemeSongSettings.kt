package org.jellyfin.androidtv.preference

/**
 * ThemeSongSettings
 *
 * Phase 1 version: just an in-memory toggle.
 *
 * Later:
 * - We'll hook this to real persisted user prefs (so it survives app restarts).
 * - We'll expose it in the actual Settings UI so the user can turn theme songs on/off.
 */
class ThemeSongSettings {

	// default true because we actually want to test this feature.
	// when we wire this into real preferences we'll replace this with a getter/setter
	// backed by SharedPreferences / DataStore / whatever the app already uses.
	var themeSongsEnabled: Boolean = true

	// later we may add:
	// var continuePlayingWhileBrowsing: Boolean = false
}
