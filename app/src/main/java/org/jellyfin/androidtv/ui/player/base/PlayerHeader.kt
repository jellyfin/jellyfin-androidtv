package org.jellyfin.androidtv.ui.player.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock

@Composable
fun PlayerHeader(
	content: @Composable ColumnScope.() -> Unit,
) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		verticalAlignment = Alignment.Top,
	) {
		Column {
			content()
		}

		Spacer(Modifier.weight(1f))

		ToolbarClock(
			modifier = Modifier.wrapContentWidth(unbounded = true)
		)
	}
}
