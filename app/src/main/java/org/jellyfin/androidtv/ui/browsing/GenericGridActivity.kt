package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class GenericGridActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, BrowseGridFragment())
			.commit()
	}
}
