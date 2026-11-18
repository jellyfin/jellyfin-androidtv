package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.GridDirection
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import java.util.UUID

@Composable
fun SettingsLibrariesDisplayGridScreen(itemId: UUID, displayPreferencesId: String) {
	val router = LocalRouter.current
	val userView = rememberUserView(itemId)
	val libraryPreferences = rememberLibraryPreferences(displayPreferencesId) ?: return
	var gridDirection by rememberPreference(libraryPreferences, LibraryPreferences.gridDirection)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(userView?.name.orEmpty().uppercase()) },
				headingContent = { Text(stringResource(R.string.grid_direction)) },
			)
		}

		items(GridDirection.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				trailingContent = { RadioButton(checked = gridDirection == entry) },
				onClick = {
					gridDirection = entry
					router.back()
				},
				modifier = Modifier
					.focusKey("grid_direction_${entry.name}", initialFocus = gridDirection == entry)
			)
		}
	}
}
