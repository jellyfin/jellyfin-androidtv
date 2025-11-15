package org.jellyfin.androidtv.data.model

import org.jellyfin.sdk.model.api.SubtitlePlaybackMode

/**
 * User preferences for audio and subtitle language selection.
 * These preferences are stored in the Jellyfin server's UserConfiguration
 * and are synchronized across all user devices.
 */
data class AudioSubtitlePreferences(
	/**
	 * Preferred audio language (ISO 639-2/T code, e.g., "eng", "fra", "spa").
	 * If null, the server will use its default behavior.
	 */
	val audioLanguagePreference: String? = null,

	/**
	 * Preferred subtitle language (ISO 639-2/T code, e.g., "eng", "fra", "spa").
	 * If null, the server will use its default behavior.
	 */
	val subtitleLanguagePreference: String? = null,

	/**
	 * Subtitle display mode determining when subtitles are shown.
	 * Default: Let the server decide based on audio language.
	 */
	val subtitleMode: SubtitlePlaybackMode = SubtitlePlaybackMode.DEFAULT,
)
