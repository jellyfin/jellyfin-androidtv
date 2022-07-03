package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class PosterSize {
	/**
	 * Smallest.
	 */
	@EnumDisplayOptions(R.string.image_size_smallest)
	SMALLEST,
	/**
	 * Small.
	 */
	@EnumDisplayOptions(R.string.image_size_small)
	SMALL,

	/**
	 * Medium.
	 */
	@EnumDisplayOptions(R.string.image_size_medium)
	MED,

	/**
	 * Large.
	 */
	@EnumDisplayOptions(R.string.image_size_large)
	LARGE,

	/**
	 * Extra Large.
	 */
	@EnumDisplayOptions(R.string.image_size_xlarge)
	X_LARGE
}
