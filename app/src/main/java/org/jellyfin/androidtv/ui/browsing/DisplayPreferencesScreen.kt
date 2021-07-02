package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import kotlinx.coroutines.runBlocking
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.androidtv.preference.LibraryDisplayPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.koin.android.ext.android.inject

class DisplayPreferencesScreen : OptionsFragment() {
	private val preferencesRepository: PreferencesRepository by inject()
	private val libraryPreferences: LibraryDisplayPreferences by lazy {
		preferencesRepository.getLibraryDisplayPreferences(preferencesId!!)
	}

	private val preferencesId by lazy { requireArguments().getString(ARG_PREFERENCES_ID) }
	private val allowViewSelection by lazy { requireArguments().getBoolean(ARG_ALLOW_VIEW_SELECTION) }

	override fun onCreate(savedInstanceState: Bundle?) {
		// TODO do this automatically?
		runBlocking { libraryPreferences.update() }

		super.onCreate(savedInstanceState)
	}

	override fun onStop() {
		super.onStop()

		// TODO do this automatically?
		runBlocking { libraryPreferences.commit() }
	}

	override val screen by lazyOptionsScreen {
		setTitle(R.string.lbl_display_preferences)

		category {
			enum<PosterSize> {
				setTitle(R.string.lbl_image_size)
				bind(libraryPreferences, LibraryDisplayPreferences.posterSize)
			}
			enum<ImageType> {
				setTitle(R.string.lbl_image_type)
				bind(libraryPreferences, LibraryDisplayPreferences.imageType)
			}
			enum<GridDirection> {
				setTitle(R.string.grid_direction)
				bind(libraryPreferences, LibraryDisplayPreferences.gridDirection)
			}

			if (allowViewSelection) {
				checkbox {
					setTitle(R.string.enable_smart_view)
					contentOn = requireContext().getString(R.string.enable_smart_view_description)
					contentOff = contentOn

					bind(libraryPreferences, LibraryDisplayPreferences.enableSmartScreen)
				}
			}
		}
	}

	companion object {
		const val ARG_ALLOW_VIEW_SELECTION = "allow_view_selection"
		const val ARG_PREFERENCES_ID = "preferences_id"
	}
}
