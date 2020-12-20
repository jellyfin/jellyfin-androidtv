package org.jellyfin.androidtv.ui.preference

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.ui.preference.screen.UserPreferencesScreen

class PreferencesActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, PreferencesFragment().apply {
				// Set screen
				arguments = bundleOf(PreferencesFragment.EXTRA_SCREEN to UserPreferencesScreen::class.qualifiedName)
			})
			.commit()
	}
}

