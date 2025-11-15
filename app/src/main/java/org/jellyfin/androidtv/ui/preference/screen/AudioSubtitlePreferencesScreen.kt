package org.jellyfin.androidtv.ui.preference.screen

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.AudioSubtitlePreferencesRepository
import org.jellyfin.androidtv.data.repository.LanguageCacheRepository
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.OptionsLanguageList
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.languageList
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.sdk.model.api.SubtitlePlaybackMode
import org.koin.android.ext.android.inject
import timber.log.Timber

class AudioSubtitlePreferencesScreen : OptionsFragment() {
	companion object {
		private const val TAG = "AudioSubtitlePrefsScreen"
	}

	private val audioSubtitlePreferencesRepository: AudioSubtitlePreferencesRepository by inject()
	private val languageCacheRepository: LanguageCacheRepository by inject()

	private var audioLanguageListInstance: OptionsLanguageList? = null
	private var subtitleLanguageListInstance: OptionsLanguageList? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Load languages from cache or server
		loadLanguages()
	}

	private fun loadLanguages() {
		lifecycleScope.launch {
			try {
				Timber.tag(TAG).d("Loading languages...")
				val result = languageCacheRepository.loadLanguages()

				result.onSuccess { languageMap ->
					Timber.tag(TAG).d("Loaded ${languageMap.size} languages")

					// Update both list instances
					val audioLanguageEntries = mutableMapOf("" to getString(R.string.pref_language_automatic))
					audioLanguageEntries.putAll(languageMap)
					audioLanguageListInstance?.updateEntries(audioLanguageEntries)

					val subtitleLanguageEntries = mutableMapOf("" to getString(R.string.pref_language_none))
					subtitleLanguageEntries.putAll(languageMap)
					subtitleLanguageListInstance?.updateEntries(subtitleLanguageEntries)
				}.onFailure { error ->
					Timber.tag(TAG).e(error, "Failed to load languages")
					Toast.makeText(
						requireContext(),
						getString(R.string.pref_audio_subtitle_preferences) + ": Failed to load languages",
						Toast.LENGTH_SHORT
					).show()
				}
			} catch (e: IllegalStateException) {
				Timber.tag(TAG).e(e, "Unexpected error loading languages")
			} catch (e: RuntimeException) {
				Timber.tag(TAG).e(e, "Unexpected error loading languages")
			}
		}
	}

	override val screen by optionsScreen {
		setTitle(R.string.pref_audio_subtitle_preferences)

		category {
			setTitle(R.string.pref_audio_subtitle_preferences)

			languageList {
				setTitle(R.string.pref_audio_language)
				setContent(R.string.pref_audio_language_description)
				entries = mapOf("" to context.getString(R.string.pref_language_automatic))

				// Store instance for later update
				audioLanguageListInstance = this

				// No need to rebuild - updateEntries() already updates the ListPreference directly
				setOnEntriesUpdated {
					Timber.d("Audio language entries updated")
				}

				bind {
					get { audioSubtitlePreferencesRepository.preferences.value.audioLanguagePreference ?: "" }
					set { value ->
						// Update repository (optimistic update + background sync)
						lifecycleScope.launch {
							audioSubtitlePreferencesRepository.updateAudioLanguage(value.ifEmpty { null })
						}
					}
					default { "" }
				}
			}

			languageList {
				setTitle(R.string.pref_subtitle_language)
				setContent(R.string.pref_subtitle_language_description)
				entries = mapOf("" to context.getString(R.string.pref_language_none))

				// Store instance for later update
				subtitleLanguageListInstance = this

				// No need to rebuild - updateEntries() already updates the ListPreference directly
				setOnEntriesUpdated {
					Timber.d("Subtitle language entries updated")
				}

				bind {
					get { audioSubtitlePreferencesRepository.preferences.value.subtitleLanguagePreference ?: "" }
					set { value ->
						// Update repository (optimistic update + background sync)
						lifecycleScope.launch {
							audioSubtitlePreferencesRepository.updateSubtitleLanguage(value.ifEmpty { null })
						}
					}
					default { "" }
				}
			}

			enum<SubtitlePlaybackMode> {
				setTitle(R.string.pref_subtitle_mode)
				setContent(R.string.pref_subtitle_mode_description)

				bind {
					get { audioSubtitlePreferencesRepository.preferences.value.subtitleMode }
					set { value ->
						// Update repository (optimistic update + background sync)
						lifecycleScope.launch {
							audioSubtitlePreferencesRepository.updateSubtitleMode(value)
						}
					}
					default { SubtitlePlaybackMode.DEFAULT }
				}
			}
		}
	}
}
