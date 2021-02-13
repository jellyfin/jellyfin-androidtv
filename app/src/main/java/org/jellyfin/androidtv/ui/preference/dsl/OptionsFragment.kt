package org.jellyfin.androidtv.ui.preference.dsl

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat

abstract class OptionsFragment : LeanbackPreferenceFragmentCompat() {
	abstract val screen: OptionsScreen

	/**
	 * Some screens show different content based on changes made in child fragments.
	 * Setting the [rebuildOnResume] property to true will automatically rebuild the screen
	 * when the fragment is resumed.
	 */
	protected var rebuildOnResume = false

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceScreen = screen.build(preferenceManager)
	}

	override fun onResume() {
		super.onResume()

		if (rebuildOnResume) screen.build(preferenceManager, preferenceScreen)
	}
}
