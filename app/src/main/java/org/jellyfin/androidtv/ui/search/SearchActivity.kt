package org.jellyfin.androidtv.ui.search

import android.R
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.replace

class SearchActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Add fragment
		supportFragmentManager
			.beginTransaction()
			.replace<LeanbackSearchFragment>(R.id.content)
			.commit()
	}

	override fun onSearchRequested(): Boolean {
		// Reset layout
		recreate()

		return true
	}
}
