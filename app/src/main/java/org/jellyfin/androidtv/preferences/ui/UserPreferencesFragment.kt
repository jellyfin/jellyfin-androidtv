package org.jellyfin.androidtv.preferences.ui

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.preferences.UserPreferences
import org.jellyfin.androidtv.preferences.ui.category.*
import org.jellyfin.androidtv.preferences.ui.preference.ButtonRemapDialogFragment
import org.jellyfin.androidtv.preferences.ui.preference.ButtonRemapPreference
import org.jellyfin.androidtv.preferences.ui.preference.EditLongPreference

class UserPreferencesFragment : LeanbackSettingsFragmentCompat() {
	override fun onPreferenceStartInitialScreen() {
		startPreferenceFragment(InnerUserPreferencesFragment())
	}

	override fun onPreferenceDisplayDialog(caller: PreferenceFragmentCompat, pref: Preference?): Boolean {
		if (pref is ButtonRemapPreference) {
			val fragment = ButtonRemapDialogFragment.newInstance(pref.key).apply {
				setTargetFragment(caller, 0)
			}
			startPreferenceFragment(fragment)

			return true
		}

		return super.onPreferenceDisplayDialog(caller, pref)
	}

	override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val fragment = childFragmentManager.fragmentFactory.instantiate(requireActivity().classLoader, pref.fragment).apply {
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

	class InnerUserPreferencesFragment : LeanbackPreferenceFragmentCompat() {
		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			val userPreferences = TvApp.getApplication().userPreferences

			preferenceScreen = preferenceManager.createPreferenceScreen(preferenceManager.context).apply {
				setTitle(R.string.settings_title)

				// Add all categories (using extension functions in the "category" folder)
				authenticationCategory(userPreferences)
				generalCategory(userPreferences)
				playbackCategory(requireActivity(), userPreferences)
				liveTvCategory(userPreferences)
				shortcutsCategory(userPreferences)
				crashReportingCategory(userPreferences)
				aboutCategory()
			}
		}

		private fun addCustomBehavior() {
			findPreference<EditLongPreference>("libvlc_audio_delay")?.apply {
				text = TvApp.getApplication().userPreferences[UserPreferences.libVLCAudioDelay].toString()
				summaryProvider = Preference.SummaryProvider<EditLongPreference> {
					"${it.text} ms"
				}
			}
		}
	}
}
