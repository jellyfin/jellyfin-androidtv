package org.jellyfin.androidtv.ui.preference

import androidx.leanback.preference.LeanbackEditTextPreferenceDialogFragmentCompat
import androidx.leanback.preference.LeanbackListPreferenceDialogFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.*
import org.jellyfin.androidtv.ui.preference.custom.ButtonRemapDialogFragment
import org.jellyfin.androidtv.ui.preference.custom.ButtonRemapPreference

class PreferencesFragment : LeanbackSettingsFragmentCompat() {
	override fun onPreferenceStartInitialScreen() = startPreferenceFragment(instantiateFragment(requireArguments().getString(EXTRA_SCREEN)!!))

	override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val fragment = instantiateFragment(pref.fragment).apply {
			setTargetFragment(caller, 0)
			arguments = pref.extras
		}

		startPreferenceFragment(fragment)

		return true
	}

	override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean = false

	override fun onPreferenceDisplayDialog(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val fragment = when (pref) {
			// Custom
			is ButtonRemapPreference -> ButtonRemapDialogFragment.newInstance(pref.key)

			// Leanback
			is ListPreference -> LeanbackListPreferenceDialogFragmentCompat.newInstanceSingle(pref.key)
			is MultiSelectListPreference -> LeanbackListPreferenceDialogFragmentCompat.newInstanceMulti(pref.key)
			is EditTextPreference -> LeanbackEditTextPreferenceDialogFragmentCompat.newInstance(pref.key)

			// Unknown fragment
			else -> return false
		}

		fragment.setTargetFragment(caller, 0)
		startPreferenceFragment(fragment)

		return true
	}

	private fun instantiateFragment(name: String) = childFragmentManager.fragmentFactory.instantiate(
		requireActivity().classLoader,
		name
	)

	companion object {
		const val EXTRA_SCREEN = "screen"
	}
}
