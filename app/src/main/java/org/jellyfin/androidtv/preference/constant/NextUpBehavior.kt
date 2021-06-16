package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.EnumDisplayOptions

enum class NextUpBehavior {
	/**
	 * Enable the Next Up screen - show the item's thumbnail
	 */
	@EnumDisplayOptions(R.string.lbl_next_up_extended)
	EXTENDED,

	/**
	 * Enable the Next Up screen - hide the item's thumbnail
	 */
	@EnumDisplayOptions(R.string.lbl_next_up_minimal)
	MINIMAL,

	/**
	 * Disable the Next Up screen
	 */
	@EnumDisplayOptions(R.string.lbl_never)
	DISABLED
}
