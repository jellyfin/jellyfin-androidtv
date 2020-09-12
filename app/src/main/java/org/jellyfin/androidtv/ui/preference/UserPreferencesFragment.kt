package org.jellyfin.androidtv.ui.preference

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.preference.category.*
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.androidtv.ui.preference.preference.ButtonRemapDialogFragment
import org.jellyfin.androidtv.ui.preference.preference.ButtonRemapPreference

class UserPreferencesFragment : LeanbackSettingsFragmentCompat() {
	class InnerUserPreferencesFragment : LeanbackPreferenceFragmentCompat() {
		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			val userPreferences = TvApp.getApplication().userPreferences

			preferenceScreen = optionsScreen(requireContext()) {
				setTitle(R.string.settings_title)

				// Add all categories (using extension functions in the "category" subpackage)
				authenticationCategory(userPreferences)
				generalCategory(userPreferences)
				playbackCategory(requireActivity(), userPreferences)
				liveTvCategory(userPreferences)
				shortcutsCategory(userPreferences)
				crashReportingCategory(userPreferences)
				aboutCategory()
			}.build(preferenceManager)
		}
	}

	override fun onPreferenceStartInitialScreen() = startPreferenceFragment(InnerUserPreferencesFragment())

	override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val fragment = childFragmentManager.fragmentFactory.instantiate(
			requireActivity().classLoader,
			pref.fragment
		).apply {
			setTargetFragment(caller, 0)
			arguments = pref.extras
		}

		val isImmersive = fragment !is PreferenceFragmentCompat && fragment !is PreferenceDialogFragmentCompat

		if (isImmersive) startImmersiveFragment(fragment)
		else startPreferenceFragment(fragment)

		return true
	}

	override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean {
		val fragment = InnerUserPreferencesFragment().apply {
			arguments = Bundle(1).apply {
				putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
			}
		}

		startPreferenceFragment(fragment)
		return true
	}

	override fun onPreferenceDisplayDialog(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		// Add dialog for shortcuts
		if (pref is ButtonRemapPreference) {
			val fragment = ButtonRemapDialogFragment.newInstance(pref.key).apply {
				setTargetFragment(caller, 0)
			}

			startPreferenceFragment(fragment)
			return true
		}

		return super.onPreferenceDisplayDialog(caller, pref)
	}
}
