package org.jellyfin.androidtv.playback.nextup

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class UpNextActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.add(android.R.id.content, UpNextFragment())
			.commit()
	}
}
