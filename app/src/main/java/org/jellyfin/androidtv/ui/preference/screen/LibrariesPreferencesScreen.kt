package org.jellyfin.androidtv.ui.preference.screen

import androidx.core.os.bundleOf
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.browsing.DisplayPreferencesScreen
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject

class LibrariesPreferencesScreen : OptionsFragment() {
	private val userViewsRepository by inject<UserViewsRepository>()

	override fun onStart() {
		super.onStart()

		userViewsRepository.views.observe(viewLifecycleOwner) { rebuild() }
	}

	override val screen by optionsScreen {
		setTitle(R.string.pref_libraries)

		category {
			userViewsRepository.views.value.orEmpty().forEach {
				val allowViewSelection = userViewsRepository.allowViewSelection(it.collectionType.orEmpty())

				link {
					title = it.name
					icon = R.drawable.ic_folder
					withFragment<DisplayPreferencesScreen>(bundleOf(
						DisplayPreferencesScreen.ARG_ALLOW_VIEW_SELECTION to allowViewSelection,
						DisplayPreferencesScreen.ARG_PREFERENCES_ID to it.displayPreferencesId,
					))
				}
			}
		}
	}
}
