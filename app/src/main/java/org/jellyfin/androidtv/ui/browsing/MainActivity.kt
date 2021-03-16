package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.home.HomeFragment
import org.jellyfin.androidtv.ui.shared.BaseActivity
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {
	private val backgroundService: BackgroundService by inject<BackgroundService>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, HomeFragment())
			.commit()

		backgroundService.attach(this)
	}
}
