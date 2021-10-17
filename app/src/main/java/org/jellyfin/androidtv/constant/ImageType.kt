package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class ImageType {
	/**
	 * Default.
	 */
	@EnumDisplayOptions(R.string.image_type_default)
	DEFAULT,

	/**
	 * Thumbnail.
	 */
	@EnumDisplayOptions(R.string.image_type_thumbnail)
	THUMB,

	/**
	 * Banner.
	 */
	@EnumDisplayOptions(R.string.image_type_banner)
	BANNER
}
