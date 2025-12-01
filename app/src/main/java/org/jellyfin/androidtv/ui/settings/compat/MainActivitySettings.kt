package org.jellyfin.androidtv.ui.settings.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.screen.SettingsMainScreen
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun MainActivitySettings() {
	val viewModel = koinActivityViewModel<SettingsViewModel>()
	val visible by viewModel.visible.collectAsState()

	JellyfinTheme {
		SettingsDialog(
			visible = visible,
			onDismissRequest = { viewModel.hide() }
		) {
			SettingsMainScreen()
		}
	}
}
