package org.jellyfin.androidtv.ui.preference.dsl

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat

abstract class OptionsFragment : LeanbackPreferenceFragmentCompat() {
	abstract val screen: OptionsScreen

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		preferenceScreen = screen.build(preferenceManager)
	}
}
