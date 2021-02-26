package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class WatchedIndicatorBehavior {
	/**
	 * Always show watched indicators.
	 */
	@EnumDisplayOptions(R.string.lbl_always)
	ALWAYS,

	/**
	 * Hide unwatched count indicator, show watched check mark only.
	 */
	@EnumDisplayOptions(R.string.lbl_hide_unwatched_count)
	HIDE_UNWATCHED,

	/**
	 * Hide unwatched count indicator, show watched check mark on individual episodes only.
	 */
	@EnumDisplayOptions(R.string.lbl_episode_marks)
	EPISODES_ONLY,

	/**
	 * Never show watched indicators.
	 */
	@EnumDisplayOptions(R.string.lbl_never)
	NEVER
}
