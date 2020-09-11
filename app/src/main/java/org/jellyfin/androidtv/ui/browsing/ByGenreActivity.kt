package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import org.jellyfin.androidtv.ui.base.BaseActivity

class ByGenreActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, ByGenreFragment())
			.commit()
	}
}
