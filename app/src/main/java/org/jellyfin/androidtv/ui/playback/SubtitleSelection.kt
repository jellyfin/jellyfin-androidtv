package org.jellyfin.androidtv.ui.playback

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.sdk.model.api.MediaStream
import java.util.UUID

/**
 * A subtitle selection made by the user. Can be used to find a matching subtitle stream in other
 * episodes of the same series.
 *
 * A selection with [disabled] set to true is used to restore the explicit choice of no subtitles.
 */
@Serializable
data class SubtitleSelection(
	/**
	 * ISO code of the subtitle language.
	 */
	val language: String? = null,
	/**
	 * Title of the subtitle stream. Preferably set to prefer the exact same stream when matching.
	 */
	val title: String? = null,
	val disabled: Boolean = false,
) {
	/**
	 * Whether the language of the selection matches the stream. Used to find fallback candidates.
	 */
	fun matchesLanguage(stream: MediaStream) = language != null && language == stream.language

	/**
	 * Whether the selection matches the stream exactly, using the stream title in addition to the
	 * language.
	 */
	fun matchesStream(stream: MediaStream) = matchesLanguage(stream) && (title == null || title == stream.title || title == stream.displayTitle)
}

private val json = Json { ignoreUnknownKeys = true }

/**
 * The most recently selected subtitle per series, persisted between sessions. Selections made in
 * the current session are also kept in [VideoQueueManager], which takes precedence because it is
 * always the most recent one.
 */
private fun UserPreferences.getSeriesSubtitleSelections() = runCatching {
	json.decodeFromString<Map<String, SubtitleSelection>>(this@getSeriesSubtitleSelections[UserPreferences.subtitlesSeriesSelections])
}.getOrDefault(emptyMap())

fun UserPreferences.getSeriesSubtitleSelection(seriesId: UUID) =
	getSeriesSubtitleSelections()[seriesId.toString()]

fun UserPreferences.setSeriesSubtitleSelection(seriesId: UUID, selection: SubtitleSelection?) {
	val selections = getSeriesSubtitleSelections().toMutableMap()
	selection?.let { selections[seriesId.toString()] = it } ?: selections.remove(seriesId.toString())

	this@setSeriesSubtitleSelection[UserPreferences.subtitlesSeriesSelections] =
		if (selections.isEmpty()) "" else json.encodeToString(selections)
}
