package org.jellyfin.androidtv.ui.preference.screen

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.browsing.DisplayPreferencesScreen
import org.jellyfin.androidtv.ui.livetv.GuideOptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.android.ext.android.inject

class LibrariesPreferencesScreen : OptionsFragment() {
	private val userViewsRepository by inject<UserViewsRepository>()
	private val userViews by lazy {
		userViewsRepository.views.stateIn(lifecycleScope, SharingStarted.Lazily, emptyList())
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		userViews.onEach {
			rebuild()
		}.launchIn(lifecycleScope)
	}

	override val screen by optionsScreen {
		setTitle(R.string.pref_libraries)

		category {
			userViews.value.forEach {
				val allowViewSelection = userViewsRepository.allowViewSelection(it.collectionType)

				link {
					title = it.name

					if (userViewsRepository.allowGridView(it.collectionType)) {
						icon = R.drawable.ic_folder
						withFragment<DisplayPreferencesScreen>(bundleOf(
							DisplayPreferencesScreen.ARG_ALLOW_VIEW_SELECTION to allowViewSelection,
							DisplayPreferencesScreen.ARG_PREFERENCES_ID to it.displayPreferencesId,
						))
					} else if (it.collectionType == CollectionType.LIVETV) {
						icon = R.drawable.ic_guide
						withFragment<GuideOptionsScreen>()
					} else {
						icon = R.drawable.ic_folder
						enabled = false
					}
				}
			}
		}
	}
}
