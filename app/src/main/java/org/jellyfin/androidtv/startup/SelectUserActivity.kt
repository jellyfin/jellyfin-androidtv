package org.jellyfin.androidtv.startup

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class SelectUserActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, SelectUserFragment())
			.commit()
	}
}
