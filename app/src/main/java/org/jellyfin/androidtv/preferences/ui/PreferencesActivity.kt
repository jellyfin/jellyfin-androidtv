package org.jellyfin.androidtv.preferences.ui

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.activity_preferences.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.Utils

class PreferencesActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_preferences)

		// Set version info
		settings_version_info?.text = Utils.getVersionString()
	}
}