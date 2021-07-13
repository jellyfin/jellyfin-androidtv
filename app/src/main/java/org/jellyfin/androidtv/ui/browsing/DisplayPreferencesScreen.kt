package org.jellyfin.androidtv.ui.browsing

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.androidtv.constant.ViewType
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
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
		TvApp.getApplication()!!.getCachedDisplayPrefs(preferencesId)
	}

	override fun onStop() {
		super.onStop()

		Timber.d("Saving cached display preferences with id $preferencesId")
		TvApp.getApplication()!!.updateDisplayPrefs(displayPreferences)
	}

	override val screen by lazyOptionsScreen {
		val allowViewSelection = requireArguments().getBoolean(ARG_ALLOW_VIEW_SELECTION)

		setTitle(R.string.lbl_display_preferences)

		category {
			list {
				setTitle(R.string.lbl_image_size)
				entries = mapOf(
					PosterSize.AUTO to requireContext().getString(R.string.image_size_auto),
					PosterSize.SMALL to requireContext().getString(R.string.image_size_small),
					PosterSize.MED to requireContext().getString(R.string.image_size_medium),
					PosterSize.LARGE to requireContext().getString(R.string.image_size_large),
				)

				bind {
					get { displayPreferences.customPrefs["PosterSize"] ?: PosterSize.AUTO }
					set { displayPreferences.customPrefs["PosterSize"] = it }
					default { PosterSize.AUTO }
				}
			}
			list {
				setTitle(R.string.lbl_image_type)
				entries = mapOf(
					ImageType.DEFAULT to requireContext().getString(R.string.image_type_default),
					ImageType.THUMB to requireContext().getString(R.string.image_type_thumbnail),
					ImageType.BANNER to requireContext().getString(R.string.image_type_banner),
				)

				bind {
					get { displayPreferences.customPrefs["ImageType"] ?: ImageType.DEFAULT }
					set { displayPreferences.customPrefs["ImageType"] = it }
					default { ImageType.DEFAULT }
				}
			}
			enum<GridDirection> {
				setTitle(R.string.grid_direction)
				bind {
					get { GridDirection.getGridDirection(displayPreferences.customPrefs["GridDirection"]) ?: GridDirection.HORIZONTAL }
					set { displayPreferences.customPrefs["GridDirection"] = it.name }
					default { GridDirection.HORIZONTAL }
				}
			}

			if (allowViewSelection) {
				checkbox {
					setTitle(R.string.enable_smart_view)
					contentOn = requireContext().getString(R.string.enable_smart_view_description)
					contentOff = contentOn

					bind {
						get { displayPreferences.customPrefs["DefaultView"] ?: ViewType.SMART == ViewType.SMART }
						set { displayPreferences.customPrefs["DefaultView"] = if (it) ViewType.SMART else ViewType.GRID }
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
