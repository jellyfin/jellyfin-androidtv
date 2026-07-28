package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsLibrariesScreen() {
	val router = LocalRouter.current
	val viewModel = koinViewModel<SettingsLibrariesScreenViewModel>()
	val userViews by viewModel.userViews.collectAsState()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_libraries)) },
			)
		}

		items(userViews) { userView ->
			val allowGridView = viewModel.allowGridView(userView.collectionType)
			val displayPreferencesId = userView.displayPreferencesId

			if (userView.collectionType == CollectionType.LIVETV) {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_guide), contentDescription = null) },
					headingContent = { Text(userView.name.orEmpty()) },
					onClick = { router.push(Routes.LIVETV_GUIDE_OPTIONS) },
					modifier = Modifier.focusKey(Routes.LIVETV_GUIDE_OPTIONS)
				)
			} else {
				val canOpen = allowGridView && displayPreferencesId != null

				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_folder), contentDescription = null) },
					headingContent = { Text(userView.name.orEmpty()) },
					enabled = canOpen,
					onClick = {
						if (canOpen) {
							router.push(
								Routes.LIBRARIES_DISPLAY,
								mapOf("itemId" to userView.id.toString(), "displayPreferencesId" to userView.displayPreferencesId!!)
							)
						}
					},
					modifier = Modifier.focusKey("library_${userView.id}")
				)
			}
		}
	}
}
