package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class ScreenSaverType(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * Sets the screensaver to normal slideshow behavior
	 */
	NORMAL(R.string.lbl_fit),

	/**
	 * Sets the screensaver to black screen
	 */
	EMPTY_SCREEN(R.string.lbl_empty_screen),
}

