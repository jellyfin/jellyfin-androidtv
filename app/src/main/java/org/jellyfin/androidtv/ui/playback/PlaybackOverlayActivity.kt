package org.jellyfin.androidtv.ui.playback

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.ui.validateAuthentication
import org.jellyfin.androidtv.util.applyTheme
import org.koin.android.ext.android.inject

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
class PlaybackOverlayActivity : FragmentActivity(R.layout.fragment_content_view) {
	private val playbackControllerContainer by inject<PlaybackControllerContainer>()
	var keyListener: View.OnKeyListener? = null

	public override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		if (!validateAuthentication()) return

		// Workaround for Sony Bravia devices that show a "grey" background on HDR videos
		// Note: Should NOT be applied to the decorView as this introduces artifacts
		window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

		// Hide system bars
		WindowCompat.setDecorFitsSystemWindows(window, false)
		WindowInsetsControllerCompat(window, findViewById(R.id.content_view)).apply {
			hide(WindowInsetsCompat.Type.systemBars())
			systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		}

		supportFragmentManager.commit {
			add<CustomPlaybackOverlayFragment>(R.id.content_view, args = intent.extras)
		}
	}

	override fun onResume() {
		super.onResume()

		if (!validateAuthentication()) return
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
		// Try listener first
		if (keyListener?.onKey(currentFocus, keyCode, event) == true)
			return true

		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
			val frag = supportFragmentManager.fragments[0]
			if (frag is CustomPlaybackOverlayFragment) {
				frag.onKeyUp(keyCode, event)
				return true
			}

		}

		val playbackController = playbackControllerContainer.playbackController

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
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
			event?.startTracking()
			return true
		}

		return super.onKeyDown(keyCode, event)
	}

	override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
			val frag = supportFragmentManager.fragments[0]
			if (frag is CustomPlaybackOverlayFragment) {
				frag.onKeyLongPress(keyCode, event)
				return true
			}

		}

		return super.onKeyLongPress(keyCode, event)
	}
}
