package org.jellyfin.androidtv.util

import org.jellyfin.androidtv.TvApp

/**
 * Current (pixel) value as display pixels
 */
val Int.dp: Int
	get() = (this * TvApp.getApplication().resources.displayMetrics.density + 0.5f).toInt()
