package org.jellyfin.androidtv.ui.playback

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.shared.BaseActivity

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
class PlaybackOverlayActivity : BaseActivity() {
	var keyListener: View.OnKeyListener? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Workaround for Sony Bravia devices that show a "grey" background on HDR videos
		// Note: Should NOT be applied to the decorView as this introduces artifacts
		window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, CustomPlaybackOverlayFragment())
			.commit()
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		// Try listener first
		if (keyListener?.onKey(currentFocus, keyCode, event) == true)
			return true

		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			val frag = supportFragmentManager.fragments.get(0)
			if (frag is CustomPlaybackOverlayFragment) {
				frag.onKeyUp(keyCode, event)
				return true
			}

		}

		val playbackController = TvApp.getApplication()?.playbackController

		when (keyCode) {
			KeyEvent.KEYCODE_MEDIA_PLAY -> playbackController?.play(0)
			KeyEvent.KEYCODE_MEDIA_PAUSE -> playbackController?.pause()
			KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> playbackController?.playPause()
			KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 -> playbackController?.skip(30000)
			KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 -> playbackController?.skip(-11000)

			// Use parent handler
			else -> return super.onKeyUp(keyCode, event)
		}

		return true
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			event?.startTracking()
			return true
		}

		return super.onKeyDown(keyCode, event)
	}

	override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			val frag = supportFragmentManager.fragments.get(0)
			if (frag is CustomPlaybackOverlayFragment) {
				frag.onKeyLongPress(keyCode, event)
				return true
			}

		}

		return super.onKeyLongPress(keyCode, event)
	}
}
