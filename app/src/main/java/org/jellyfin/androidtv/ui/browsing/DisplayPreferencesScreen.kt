package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.list
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen

class DisplayPreferencesScreen : OptionsFragment() {
	// Requires the caller of this view to pre-cache the display preferences
	private val displayPreferences by lazy {
		val id = requireArguments().getString(ARG_PREFERENCES_ID)
		TvApp.getApplication().getCachedDisplayPrefs(id)
	}

	override val screen by optionsScreen {
		val allowViewSelection = requireArguments().getBoolean(ARG_ALLOW_VIEW_SELECTION)

		setTitle(R.string.lbl_display_preferences)

		category {
			list {
				setTitle(R.string.lbl_image_size)
				entries = mapOf(
					"0" to requireContext().getString(R.string.image_size_auto),
					"1" to requireContext().getString(R.string.image_size_small),
					"2" to requireContext().getString(R.string.image_size_medium),
					"3" to requireContext().getString(R.string.image_size_large),
				)

				bind {
					get { displayPreferences.getCustomPrefs().get("PosterSize") ?: "0" }
					set { displayPreferences.getCustomPrefs().set("PosterSize", it) }
					default { "0" }
				}
			}
			list {
				setTitle(R.string.lbl_image_type)
				entries = mapOf(
					"0" to requireContext().getString(R.string.image_type_default),
					"1" to requireContext().getString(R.string.image_type_thumbnail),
					"2" to requireContext().getString(R.string.image_type_banner),
				)

				bind {
					get { displayPreferences.getCustomPrefs().get("ImageType") ?: "0" }
					set { displayPreferences.getCustomPrefs().set("ImageType", it) }
					default { "0" }
				}
			}

			if (allowViewSelection) {
				checkbox {
					setTitle(R.string.enable_smart_view)
					contentOn = requireContext().getString(R.string.enable_smart_view_description)
					contentOff = contentOn

					bind {
						get { displayPreferences.getCustomPrefs().get("DefaultView") == "0" }
						set { displayPreferences.getCustomPrefs().set("DefaultView", if (it) "0" else "1") }
						default { true }
					}
				}
			}
		}
	}

	companion object {
		const val ARG_ALLOW_VIEW_SELECTION = "allow_view_selection"
		const val ARG_PREFERENCES_ID = "preferences_id"
	}
}
