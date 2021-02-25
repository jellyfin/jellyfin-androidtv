package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class RatingType {
	/**
	 * Sets default rating type to tomatoes.
	 */
	@EnumDisplayOptions(R.string.lbl_tomatoes)
	RATING_TOMATOES,

	/**
	 * Sets the default rating type to stars.
	 */
	@EnumDisplayOptions(R.string.lbl_stars)
	RATING_STARS,

	/**
	 * Sets the default rating type to hidden.
	 */
	@EnumDisplayOptions(R.string.lbl_hidden)
	RATING_HIDDEN
}
