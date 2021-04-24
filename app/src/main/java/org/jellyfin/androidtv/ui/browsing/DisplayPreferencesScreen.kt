package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.list
import timber.log.Timber

class DisplayPreferencesScreen : OptionsFragment() {
	private val preferencesId by lazy {
		requireArguments().getString(ARG_PREFERENCES_ID)
	}

	// Requires the caller of this view to pre-cache the display preferences
	private val displayPreferences by lazy {
		Timber.d("Loading cached display preferences with id $preferencesId")
		TvApp.getApplication().getCachedDisplayPrefs(preferencesId)
	}

	override fun onStop() {
		super.onStop()

		Timber.d("Saving cached display preferences with id $preferencesId")
		TvApp.getApplication().updateDisplayPrefs(displayPreferences)
	}

	override val screen by lazyOptionsScreen {
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
			list {
				setTitle(R.string.grid_direction)
				entries = mapOf(
					GridDirection.HORIZONTAL.name to requireContext().getString(R.string.grid_direction_horizontal),
					GridDirection.VERTICAL.name to requireContext().getString(R.string.grid_direction_vertical),
				)

				bind {
					get { displayPreferences.getCustomPrefs().get("GridDirection") ?: GridDirection.HORIZONTAL.name }
					set { displayPreferences.getCustomPrefs().set("GridDirection", it) }
					default { GridDirection.HORIZONTAL.name }
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
