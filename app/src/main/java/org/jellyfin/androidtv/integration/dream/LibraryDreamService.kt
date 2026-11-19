package org.jellyfin.androidtv.integration.dream

import android.content.Intent
import android.service.dreams.DreamService
import android.view.KeyEvent
import org.jellyfin.androidtv.integration.dream.composable.DreamHost
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.isOpenItemKeyEvent

/**
 * An Android [DreamService] (screensaver) that shows TV series and movies from all libraries.
 * Pressing play while a library item is shown opens the details of that item in the app.
 * Use `adb shell am start -n "com.android.systemui/.Somnambulator"` to start after changing the
 * default screensaver in the device settings.
 */
class LibraryDreamService : DreamServiceCompat() {
	private var currentContent: DreamContent? = null

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()

		isInteractive = true
		isFullscreen = true

		setContent {
			DreamHost(onContentChange = { currentContent = it })
		}
	}

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		val content = currentContent

		if (content is DreamContent.LibraryShowcase && event.isOpenItemKeyEvent()) {
			if (event.action == KeyEvent.ACTION_UP) {
				startActivity(
					Intent(this, StartupActivity::class.java)
						.putExtra(StartupActivity.EXTRA_ITEM_ID, content.item.id.toString())
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				)
				finish()
			}
		} else {
			// Same behavior as a non-interactive dream: any other key wakes up
			wakeUp()
		}

		return true
	}
}
