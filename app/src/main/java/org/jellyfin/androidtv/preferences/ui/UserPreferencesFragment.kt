package org.jellyfin.androidtv.preferences.ui

import android.app.AlertDialog
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.*
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.model.LogonCredentials
import org.jellyfin.androidtv.util.DeviceUtils
import org.jellyfin.androidtv.util.apiclient.AuthenticationHelper
import java.io.IOException

class UserPreferencesFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
	private val extPlayerVideoDep = arrayOf(
			"pref_enable_cinema_mode",
			"pref_refresh_switching",
			"pref_audio_option",
			"pref_bitstream_ac3",
			"pref_bitstream_dts"
	)
	private val extPlayerLiveTvDep = arrayOf(
			"pref_live_direct",
			"pref_enable_vlc_livetv"
	)

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		// Temporary workaround to get dialogs working (would crash when using Leanback theme)
		context?.setTheme(R.style.Theme_AppCompat)
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.preferences, rootKey)

		// Remove preferences not available for current device
		findPreference<PreferenceCategory>("pref_playback_category")?.apply {
			if (DeviceUtils.isFireTv() && !DeviceUtils.is50()) removePreference(findPreference("pref_audio_option"))
			if (DeviceUtils.is60()) removePreference(findPreference("pref_bitstream_ac3"))
			if (!DeviceUtils.is60()) removePreference(findPreference("pref_refresh_switching"))
		}

		updateAllDependencies()
	}

	override fun onResume() {
		super.onResume()

		preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

		updateAllDependencies()
	}

	override fun onPause() {
		super.onPause()

		preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
		// If logon setting is changed to this user, save credentials
		if (key == "pref_login_behavior") {
			val listPreference = findPreference<ListPreference>(key)

			if (listPreference?.value == "1") {
				try {
					val credentials = LogonCredentials(TvApp.getApplication().apiClient.serverInfo, TvApp.getApplication().currentUser)
					AuthenticationHelper.saveLoginCredentials(credentials, TvApp.CREDENTIALS_PATH)
				} catch (e: IOException) {
					TvApp.getApplication().logger.ErrorException("Unable to save logon credentials", e)
				}
			}
		}

		// Show warning when changing external path option
		if (key == "pref_send_path_external" && findPreference<CheckBoxPreference>(key)!!.isChecked) {
			AlertDialog.Builder(activity)
					.setTitle(getString(R.string.lbl_warning))
					.setMessage(getString(R.string.msg_external_path))
					.setPositiveButton(R.string.btn_got_it, null)
					.show()
		}

		// Update preference dependencies
		findPreference<Preference>(key)?.let(::updatePreferenceDependencies)


		TvApp.getApplication().logger.Info("DOWNMIX STATE: " + TvApp.getApplication().userPreferences.audioBehaviour.toString())
	}

	private fun updateAllDependencies() = updateGroupDependencies(preferenceScreen)
	private fun updateGroupDependencies(group: PreferenceGroup) {
		for (i in 0 until group.preferenceCount) {
			val preference = group.getPreference(i)

			if (preference is PreferenceGroup) updateGroupDependencies(preference)
			else updatePreferenceDependencies(preference)
		}
	}

	private fun updatePreferenceDependencies(preference: Preference) {
		if (preference is ListPreference) {
			if (preference.key == "pref_login_behavior") {
				val pwPrompt = findPreference<CheckBoxPreference>("pref_auto_pw_prompt")

				pwPrompt?.isEnabled = TvApp.getApplication().configuredAutoCredentials.userDto.hasPassword

				var extra = ""
				if (preference.value == "1") {
					if (TvApp.getApplication().configuredAutoCredentials.userDto.id != TvApp.getApplication().currentUser.id) {
						preference.isEnabled = false
						pwPrompt?.isEnabled = false
						extra = activity?.getString(R.string.lbl_paren_login_as) + TvApp.getApplication().configuredAutoCredentials.userDto.name + activity?.getString(R.string.lbl_to_change_paren)
					}
					preference.summary = activity?.getString(R.string.lbl_login_as) + TvApp.getApplication().configuredAutoCredentials.userDto.name + extra
				} else {
					preference.summary = preference.entry
					pwPrompt?.isEnabled = false
				}
			} else if (preference.key == "pref_video_player") {
				val isExternal = preference.value == "external"
				// enable/disable other related items
				val direct = findPreference<Preference>("pref_send_path_external")
				if (direct != null) direct.isEnabled = isExternal
				for (key in extPlayerVideoDep) {
					val pref = findPreference<Preference>(key)
					if (pref != null) pref.isEnabled = !isExternal
				}
			} else {
				preference.summary = preference.entry
			}
		}

		if (preference is CheckBoxPreference) {
			if (preference.key == "pref_live_direct") {
				// Enable other live tv direct only options
				val live = findPreference<Preference>("pref_enable_vlc_livetv")
				if (live != null) live.isEnabled = preference.isChecked
			} else if (preference.key == "pref_live_tv_use_external") {
				// Enable / disable other related items
				for (key in extPlayerLiveTvDep)
					findPreference<Preference>(key)?.isEnabled = !preference.isChecked
			}
		}
	}
}