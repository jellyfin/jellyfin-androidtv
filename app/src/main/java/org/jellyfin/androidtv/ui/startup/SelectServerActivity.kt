package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import org.jellyfin.androidtv.ui.shared.BaseActivity

class SelectServerActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, SelectServerFragment())
			.commit()
	}
}
