package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jellyfin.androidtv.ui.base.dialog.DialogBase

@Composable
fun SettingsDialog(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	screen: @Composable BoxScope.() -> Unit,
) {
	DialogBase(
		visible = visible,
		onDismissRequest = onDismissRequest,
		modifier = modifier,
	) {
		SettingsLayout(
			modifier = Modifier
				.align(Alignment.TopEnd),
		) {
			screen()
		}
	}
}
