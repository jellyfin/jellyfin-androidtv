package org.jellyfin.androidtv.browsing

import android.os.Bundle
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.base.BaseActivity

class MainActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_home)
	}
}