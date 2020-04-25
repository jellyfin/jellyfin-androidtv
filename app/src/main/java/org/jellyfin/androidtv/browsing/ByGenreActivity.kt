package org.jellyfin.androidtv.browsing

import android.os.Bundle
import org.jellyfin.androidtv.base.BaseActivity

class ByGenreActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, ByGenreFragment())
			.commit()
	}
}
