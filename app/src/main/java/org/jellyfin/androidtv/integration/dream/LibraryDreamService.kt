package org.jellyfin.androidtv.integration.dream

import android.service.dreams.DreamService
import org.jellyfin.androidtv.integration.dream.composable.DreamHost

/**
 * An Android [DreamService] (screensaver) that shows TV series and movies from all libraries.
 * Use `adb shell am start -n "com.android.systemui/.Somnambulator"` to start after changing the
 * default screensaver in the device settings.
 */
class LibraryDreamService : DreamServiceCompat() {
	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		isInteractive = false
		isFullscreen = true

		setContent {
			DreamHost()
		}
	}
}
