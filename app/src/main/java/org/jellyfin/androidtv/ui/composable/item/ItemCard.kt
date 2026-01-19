package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import org.jellyfin.androidtv.ui.base.JellyfinTheme

@Composable
@Stable
fun ItemCard(
	modifier: Modifier = Modifier,
	image: @Composable BoxScope.() -> Unit,
	overlay: (@Composable BoxScope.() -> Unit)? = null,
	shape: Shape = JellyfinTheme.shapes.medium,
) {
	Box(
		modifier = modifier
			.clip(shape)
			.background(JellyfinTheme.colorScheme.surface, shape)
	) {
		image()

		if (overlay != null) {
			Box(
				modifier = Modifier.fillMaxSize(),
				content = overlay
			)
		}
	}
}
