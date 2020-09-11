package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import org.jellyfin.androidtv.ui.base.BaseActivity

class BrowsePersonsActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, BrowsePersonsFragment())
			.commit()
	}
}
