package org.jellyfin.androidtv.ui.playback.external

import kotlin.time.Duration

sealed interface ExternalPlayResult {
	data class Success(
		/**
		 * The position that playback ended on.
		 */
		val position: Duration? = null,

		/**
		 * Whether the playback completed to the end.
		 */
		val completed: Boolean? = null,
	) : ExternalPlayResult

	data object Failed : ExternalPlayResult
}
