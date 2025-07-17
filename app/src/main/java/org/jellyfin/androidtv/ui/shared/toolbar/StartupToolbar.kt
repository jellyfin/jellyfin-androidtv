package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.button.IconButton

@Composable
fun StartupToolbar(
	openHelp: () -> Unit,
	openSettings: () -> Unit,
) {
	Toolbar(
		end = {
			ToolbarButtons {
				IconButton(onClick = openHelp) {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_help),
						contentDescription = stringResource(R.string.help),
					)
				}

				IconButton(onClick = openSettings) {
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
}
