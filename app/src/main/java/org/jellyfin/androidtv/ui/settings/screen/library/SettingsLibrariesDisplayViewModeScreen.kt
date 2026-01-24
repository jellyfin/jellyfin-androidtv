package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ViewMode
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.preference.ListItemHeight
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

/**
 * Settings screen for selecting the view mode (Grid or List)
 */
@Composable
fun SettingsLibrariesDisplayViewModeScreen(itemId: UUID, displayPreferencesId: String) {
    val router = LocalRouter.current
    val preferencesRepository = koinInject<PreferencesRepository>()
    val userView = rememberUserView(itemId)
    val libraryPreferences = remember(displayPreferencesId) { 
        preferencesRepository.getLibraryPreferences(displayPreferencesId) 
    }
    var viewMode by rememberPreference(libraryPreferences, LibraryPreferences.viewMode)

    SettingsColumn {
        item {
            ListSection(
                overlineContent = { Text(userView?.name.orEmpty().uppercase()) },
                headingContent = { Text(stringResource(R.string.view_mode)) },
            )
        }

        items(ViewMode.entries) { entry ->
            ListButton(
                headingContent = { Text(stringResource(entry.nameRes)) },
                trailingContent = { RadioButton(checked = viewMode == entry) },
                onClick = {
                    viewMode = entry
                    router.back()
                }
            )
        }
    }
}

/**
 * Settings screen for selecting list item height
 */
@Composable
fun SettingsLibrariesDisplayListHeightScreen(itemId: UUID, displayPreferencesId: String) {
    val router = LocalRouter.current
    val preferencesRepository = koinInject<PreferencesRepository>()
    val userView = rememberUserView(itemId)
    val libraryPreferences = remember(displayPreferencesId) { 
        preferencesRepository.getLibraryPreferences(displayPreferencesId) 
    }
    var listItemHeight by rememberPreference(libraryPreferences, LibraryPreferences.listItemHeight)

    SettingsColumn {
        item {
            ListSection(
                overlineContent = { Text(userView?.name.orEmpty().uppercase()) },
                headingContent = { Text(stringResource(R.string.list_item_height)) },
            )
        }

        items(ListItemHeight.entries) { entry ->
            ListButton(
                headingContent = { Text(stringResource(entry.nameRes)) },
                trailingContent = { RadioButton(checked = listItemHeight == entry) },
                onClick = {
                    listItemHeight = entry
                    router.back()
                }
            )
        }
    }
}
