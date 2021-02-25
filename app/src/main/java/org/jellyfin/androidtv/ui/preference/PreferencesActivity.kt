package org.jellyfin.androidtv.ui.preference

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.ui.preference.screen.UserPreferencesScreen

class PreferencesActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val screen = intent.extras?.getString(EXTRA_SCREEN) ?: UserPreferencesScreen::class.qualifiedName
		val screenArgs = intent.extras?.getBundle(EXTRA_SCREEN_ARGS) ?: bundleOf()

		supportFragmentManager
			.beginTransaction()
			.replace(android.R.id.content, PreferencesFragment().apply {
				// Set screen
				arguments = bundleOf(
					PreferencesFragment.EXTRA_SCREEN to screen,
					PreferencesFragment.EXTRA_SCREEN_ARGS to screenArgs
				)
			}, FRAGMENT_TAG)
			.commit()
	}

	companion object {
		const val EXTRA_SCREEN = "screen"
		const val EXTRA_SCREEN_ARGS = "screen_args"
		const val FRAGMENT_TAG = "PreferencesActivity"
	}
}

