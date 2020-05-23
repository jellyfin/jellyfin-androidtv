package org.jellyfin.androidtv.preferences.enums

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.ui.dsl.EnumDisplayOptions

enum class GridDirection {
	/**
	 * Horizontal
	 */
	@EnumDisplayOptions(R.string.grid_direction_horizontal)
	HORIZONTAL,

	/**
	 * Vertical
	 */
	@EnumDisplayOptions(R.string.grid_direction_vertical)
	VERTICAL
}
