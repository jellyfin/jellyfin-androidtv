package org.jellyfin.androidtv.ui.base.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

@Composable
fun ListMessage(
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit,
) {
	Box(
		modifier = modifier
			// TODO: Add suitable space token for this padding
			.padding(12.dp),
	) {
		ProvideTextStyle(LocalTextStyle.current.copy(color = JellyfinTheme.colorScheme.listCaption)) {
			content()
		}
	}
}
