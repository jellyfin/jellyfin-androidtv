package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class RatingType(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Sets default rating type to tomatoes.
	 */
	RATING_TOMATOES(R.string.lbl_tomatoes),

	/**
	 * Sets the default rating type to stars.
	 */
	RATING_STARS(R.string.lbl_stars),

	/**
	 * Sets the default rating type to hidden.
	 */
	RATING_HIDDEN(R.string.lbl_hidden),

	/**
	 * Sets the default rating type to only display ratings in the media info screen.
	 */
	RATING_ONLY_IN_MEDIA_INFO(R.string.lbl_only_in_media_info),
}
