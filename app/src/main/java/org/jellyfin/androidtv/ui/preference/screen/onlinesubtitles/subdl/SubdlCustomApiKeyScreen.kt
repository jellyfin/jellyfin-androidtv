package org.jellyfin.androidtv.ui.preference.screen.onlinesubtitles.subdl

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.onlinesubtitles.opensubtitles.OpenSubtitlesClient
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.preference.custom.ButtonWithProgressbarPreference
import org.jellyfin.androidtv.ui.preference.custom.InfoTextPreference
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.preference.store.PreferenceStore
import org.koin.android.ext.android.inject

class SubdlCustomApiKeyScreen : OptionsFragment() {

	private val userSettingPreferences: UserSettingPreferences by inject()
	private val userPreferences: UserPreferences by inject()

	override val screen: OptionsScreen
		get() =  OptionsScreen(requireContext())

	override val stores: Array<PreferenceStore<*, *>>
		get() = arrayOf(userSettingPreferences)

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.subdl_custom_apikey_preferences_screen, rootKey)

		val apiKeyPref = findPreference<EditTextPreference>("subdl_api_key")
		apiKeyPref?.text = userPreferences[UserPreferences.subdlCustomApiKey]
		val loginButtonPref = findPreference<ButtonWithProgressbarPreference>("subdl_save_button")

		loginButtonPref?.setOnPreferenceClickListener {

			val apiKey = apiKeyPref?.text ?: ""
			userPreferences[UserPreferences.subdlCustomApiKey] = apiKey
			this.parentFragmentManager.popBackStack()
			true
		}
	}
}
