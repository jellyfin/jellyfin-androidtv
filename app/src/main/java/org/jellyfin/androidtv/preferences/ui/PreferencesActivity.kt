package org.jellyfin.androidtv.preferences.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class PreferencesActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, UserPreferencesFragment())
			.commit()
	}
}
