package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.PosterSize
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun SettingsLibrariesDisplayImageSizeScreen(itemId: UUID, displayPreferencesId: String) {
	val router = LocalRouter.current
	val preferencesRepository = koinInject<PreferencesRepository>()
	val userView = rememberUserView(itemId)
	val libraryPreferences = remember(displayPreferencesId) { preferencesRepository.getLibraryPreferences(displayPreferencesId) }
	var posterSize by rememberPreference(libraryPreferences, LibraryPreferences.posterSize)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(userView?.name.orEmpty().uppercase()) },
				headingContent = { Text(stringResource(R.string.lbl_image_size)) },
			)
		}

		items(PosterSize.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				trailingContent = { RadioButton(checked = posterSize == entry) },
				onClick = {
					posterSize = entry
					router.back()
				}
			)
		}
	}
}
