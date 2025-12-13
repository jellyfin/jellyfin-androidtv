package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.androidtv.data.model.AudioSubtitlePreferences
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode

/**
 * Repository for managing user's global audio and subtitle language preferences.
 * These preferences are stored in the Jellyfin server's UserConfiguration and
 * are synchronized across all devices for the current user.
 */
interface AudioSubtitlePreferencesRepository {
	/**
	 * Current audio and subtitle preferences for the logged-in user.
	 * Updates automatically when the user changes or preferences are modified.
	 */
	val preferences: StateFlow<AudioSubtitlePreferences>

	/**
	 * Refresh preferences from the server.
	 * This is called automatically when the user changes, but can be called manually
	 * if needed (e.g., after external changes via web client).
	 */
	suspend fun refreshPreferences()

	/**
	 * Update the preferred audio language for the current user.
	 * @param language ISO 639-2/T language code (e.g., "eng", "fra") or null for server default.
	 */
	suspend fun updateAudioLanguage(language: String?)

	/**
	 * Update the preferred subtitle language for the current user.
	 * @param language ISO 639-2/T language code (e.g., "eng", "fra") or null for no default.
	 */
	suspend fun updateSubtitleLanguage(language: String?)

	/**
	 * Update the subtitle display mode for the current user.
	 * @param mode The subtitle playback mode (DEFAULT, ALWAYS, ONLY_FORCED, NONE, SMART).
	 */
	suspend fun updateSubtitleMode(mode: SubtitlePlaybackMode)
}
