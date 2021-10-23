package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import androidx.fragment.app.add
import androidx.fragment.app.replace
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.home.HomeFragment
import org.jellyfin.androidtv.ui.home.HomeToolbarFragment
import org.jellyfin.androidtv.ui.itempreview.ItemPreviewFragment
import org.jellyfin.androidtv.ui.shared.BaseActivity
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity(R.layout.fragment_content_view) {
	private val backgroundService: BackgroundService by inject<BackgroundService>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace<HomeToolbarFragment>(R.id.content_view)
			.add<ItemPreviewFragment>(R.id.content_view)
			.add<HomeFragment>(R.id.content_view)
			.commit()

		backgroundService.attach(this)
	}
}
