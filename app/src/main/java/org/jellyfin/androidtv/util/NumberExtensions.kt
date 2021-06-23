package org.jellyfin.androidtv.util

import org.jellyfin.androidtv.TvApp

/**
 * Current (pixel) value as display pixels
 */
val Int.dp: Int
	get() = Utils.convertDpToPixel(TvApp.getApplication()!!, this)
