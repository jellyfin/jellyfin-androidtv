package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.navigation.ProvideRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsDialog
import org.jellyfin.androidtv.ui.settings.composable.SettingsRouterContent
import org.jellyfin.androidtv.ui.settings.routes

@Composable
fun StartupToolbar(
	openHelp: () -> Unit
) {
	var settingsVisible by remember { mutableStateOf(false) }

	Toolbar(
		end = {
			ToolbarButtons {
				IconButton(onClick = openHelp) {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_help),
						contentDescription = stringResource(R.string.help),
					)
				}

				IconButton(onClick = { settingsVisible = true }) {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
						contentDescription = stringResource(R.string.lbl_settings),
					)
				}

				Spacer(Modifier.width(8.dp))

				ToolbarClock()
			}
		}
	)


	ProvideRouter(routes, Routes.AUTHENTICATION_FROM_LOGIN) {
		SettingsDialog(
			visible = settingsVisible,
			onDismissRequest = { settingsVisible = false }
		) {
			SettingsRouterContent()
		}
	}
}
