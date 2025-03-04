package org.jellyfin.androidtv.ui.preference.screen.onlinesubtitles.subdl

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceCategory
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.onlinesubtitles.subdl.SubdlClient
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.preference.custom.InfoTextPreference
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.preference.store.PreferenceStore
import org.koin.android.ext.android.inject

class SubdlLanguagesScreen : OptionsFragment() {

	private val subdlClient: SubdlClient by inject()
	private val userSettingPreferences: UserSettingPreferences by inject()
	private val userPreferences: UserPreferences by inject()

	override val screen: OptionsScreen
		get() = OptionsScreen(requireContext())

	override val stores: Array<PreferenceStore<*, *>>
		get() = arrayOf(userSettingPreferences)

	var langSet = HashSet<String>()

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.subdl_language_preferences_screen, rootKey)

		requestLanguagesInfo()
	}

	private fun requestLanguagesInfo() {

		val infoTextPref = findPreference<InfoTextPreference>("os_info_text")

		lifecycleScope.launch {

			val result = subdlClient.getSupportedLanguages()
			result.onSuccess { info ->

				val languageCategory = findPreference<PreferenceCategory>("languages")

				info.forEach { (language_code, language_name) ->
					val checkBox = CheckBoxPreference(preferenceManager.context).apply {
						key = "os_key_${language_code}"
						title = language_name
						summary = language_code
						isChecked = false // Varsayılan olarak seçili değil
					}
					languageCategory?.addPreference(checkBox)

					if(checkBox.isChecked){
						langSet.add(language_code)
					}

					checkBox.setOnPreferenceChangeListener { preference, newValue -> //
						val willBeChecked = !checkBox.isChecked
						if(willBeChecked){
							langSet.add(language_code)
						}else{
							langSet.remove(language_code)
						}

						val sortedLanguages = langSet.sorted().joinToString(",")
						infoTextPref?.title = sortedLanguages
						userPreferences[UserPreferences.subdlPreferredLanguages] = sortedLanguages
						true
					}
				}

				val sortedLanguages = langSet.sorted().joinToString(",")
				infoTextPref?.title = sortedLanguages

			}.onFailure {
				infoTextPref?.title = 	"Fail" ;

			}
		}
	}
}
