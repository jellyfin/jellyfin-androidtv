package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.button.IconButton

@Composable
fun StartupToolbar(
	openHelp: () -> Unit,
	openSettings: () -> Unit,
) {
	Toolbar {
		ToolbarButtons {
			IconButton(onClick = openHelp) {
				Icon(
					painter = painterResource(R.drawable.ic_help),
					contentDescription = stringResource(R.string.help),
				)
			}

			IconButton(onClick = openSettings) {
				Icon(
					painter = painterResource(R.drawable.ic_settings),
					contentDescription = stringResource(R.string.lbl_settings),
				)
			}
		}
	}
}
