package org.jellyfin.androidtv.preferences.ui

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.LogonCredentials
import org.jellyfin.androidtv.preferences.enums.LoginBehavior
import org.jellyfin.androidtv.preferences.enums.PreferredVideoPlayer
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper
import timber.log.Timber
import java.io.IOException

class UserPreferencesFragment : LeanbackSettingsFragmentCompat() {
	override fun onPreferenceStartInitialScreen() {
		startPreferenceFragment(InnerUserPreferencesFragment())
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
		private val externalPlayerDependencies = mapOf(
			"pref_send_path_external" to true,
			"pref_enable_cinema_mode" to false,
			"pref_refresh_switching" to false,
			"audio_behavior" to false,
			"pref_bitstream_ac3" to false,
			"pref_bitstream_dts" to false
		)

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.preferences, rootKey)

			updatePreferenceVisibility()
			addCustomBehavior()
		}

		override fun onResume() {
			super.onResume()
			updatePreferenceVisibility()
		}

		private fun updatePreferenceVisibility() {
			// Hide preferences not available for device
			if (DeviceUtils.isFireTv() && !DeviceUtils.is50()) findPreference<Preference>("pref_audio_option")?.isVisible = false
			if (DeviceUtils.is60()) findPreference<Preference>("pref_bitstream_ac3")?.isVisible = false
			if (!DeviceUtils.is60()) findPreference<Preference>("pref_refresh_switching")?.isVisible = false

			// Update preference with custom dependencies
			findPreference<CheckBoxPreference>("pref_auto_pw_prompt")?.isEnabled = TvApp.getApplication().configuredAutoCredentials.userDto.hasPassword

			val isExternal = TvApp.getApplication().userPreferences.videoPlayer == PreferredVideoPlayer.EXTERNAL
			for ((key, enable) in externalPlayerDependencies)
				findPreference<Preference>(key)?.isEnabled = if (enable) isExternal else !isExternal

			val isAutoLogin = TvApp.getApplication().userPreferences.loginBehavior == LoginBehavior.AUTO_LOGIN
			if (isAutoLogin && TvApp.getApplication().configuredAutoCredentials.userDto.id != TvApp.getApplication().currentUser.id) {
				// Auto-login set to another user
				findPreference<Preference>("login_behavior")?.isEnabled = false
				findPreference<Preference>("pref_auto_pw_prompt")?.isEnabled = false
			} else if (!isAutoLogin) {
				findPreference<CheckBoxPreference>("pref_auto_pw_prompt")?.isEnabled = false
			}
		}

		private fun addCustomBehavior() {
			// Custom save actions
			findPreference<ListPreference>("login_behavior")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
				if (value == LoginBehavior.AUTO_LOGIN.toString()) {
					try {
						val credentials = LogonCredentials(TvApp.getApplication().apiClient.serverInfo, TvApp.getApplication().currentUser)
						AuthenticationHelper.saveLoginCredentials(credentials, TvApp.CREDENTIALS_PATH)
					} catch (e: IOException) {
						Timber.e(e, "Unable to save logon credentials")
					}
				}

				return@OnPreferenceChangeListener true
			}

			findPreference<CheckBoxPreference>("pref_send_path_external")?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
				if (value == true) {
					AlertDialog.Builder(activity)
						.setTitle(getString(R.string.lbl_warning))
						.setMessage(getString(R.string.msg_external_path))
						.setPositiveButton(R.string.btn_got_it, null)
						.show()
				}

				return@OnPreferenceChangeListener true
			}

			// Custom summary providers
			findPreference<ListPreference>("login_behavior")?.summaryProvider = Preference.SummaryProvider<ListPreference> { preference ->
				if (preference.value == LoginBehavior.AUTO_LOGIN.toString()) {
					var extra = ""

					if (TvApp.getApplication().configuredAutoCredentials.userDto.id != TvApp.getApplication().currentUser.id)
						extra = activity?.getString(R.string.lbl_paren_login_as) + TvApp.getApplication().configuredAutoCredentials.userDto.name + activity?.getString(R.string.lbl_to_change_paren)

					return@SummaryProvider activity?.getString(R.string.lbl_login_as) + TvApp.getApplication().configuredAutoCredentials.userDto.name + extra
				}

				return@SummaryProvider preference.entry
			}

			findPreference<EditTextPreference>("version")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
				Utils.getVersionString()
			}

			findPreference<EditTextPreference>("device_model")?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
				"${Build.MANUFACTURER} ${Build.MODEL}"
			}

			findPreference<EditLongPreference>("libvlc_audio_delay")?.apply {
				text = TvApp.getApplication().userPreferences.libVLCAudioDelay.toString()
				summaryProvider = Preference.SummaryProvider<EditLongPreference> {
					"${it.text} ms"
				}
			}
		}
	}
}
