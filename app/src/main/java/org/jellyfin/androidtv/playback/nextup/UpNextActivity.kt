package org.jellyfin.androidtv.playback.nextup

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager

class UpNextActivity : FragmentActivity() {
	private lateinit var fragment: UpNextFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		fragment = UpNextFragment()

		BackgroundManager.getInstance(this).attach(window)

		supportFragmentManager
			.beginTransaction()
			.add(android.R.id.content, fragment)
			.commit()
	}

	override fun onBackPressed() {
		if (fragment.isCountdownActive()) {
			fragment.stopCountdown()
		} else {
			super.onBackPressed()
		}
	}
}
