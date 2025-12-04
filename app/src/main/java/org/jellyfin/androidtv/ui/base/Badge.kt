package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun Badge(
	modifier: Modifier = Modifier,
	shape: Shape = CircleShape,
	containerColor: Color = JellyfinTheme.colorScheme.badge,
	contentColor: Color = JellyfinTheme.colorScheme.onBadge,
	content: @Composable BoxScope.() -> Unit,
) {
	ProvideTextStyle(JellyfinTheme.typography.badge.copy(color = contentColor)) {
		Box(
			modifier = modifier
				.defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
				.wrapContentSize()
				.background(containerColor, shape)
				.padding(horizontal = 6.dp, vertical = 3.dp),
			contentAlignment = Alignment.Center,
		) {
			content()
		}
	}
}
