package org.jellyfin.androidtv.ui.playback

import org.jellyfin.androidtv.ui.ScreensaverViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class PlaybackOverlayFragmentHelper(
	val fragment: CustomPlaybackOverlayFragment
) {
	private val screensaverViewModel by fragment.activityViewModel<ScreensaverViewModel>()
	private var screensaverLock: (() -> Unit)? = null

	fun setScreensaverLock(enabled: Boolean) {
		if (enabled && screensaverLock == null) {
			screensaverLock = screensaverViewModel.addLifecycleLock(fragment.lifecycle)
		} else if (!enabled) {
			screensaverLock?.invoke()
			screensaverLock = null
		}
	}
}
