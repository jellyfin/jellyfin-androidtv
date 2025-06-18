package org.jellyfin.androidtv.integration.dream.composable

import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.ZoomBox
import org.jellyfin.androidtv.ui.composable.modifier.overscan

@Composable
fun DreamContentLibraryShowcase(
	content: DreamContent.LibraryShowcase,
) = Box(
	modifier = Modifier.fillMaxSize(),
) {
	ZoomBox(
		initialValue = 1f,
		targetValue = 1.1f,
		delayMillis = 1_000,
		durationMillis = 30_000,
	) {
		AsyncImage(
			model = content.backdropUrl,
			contentDescription = null,
			alignment = Alignment.Center,
			contentScale = ContentScale.Crop,
			modifier = Modifier.fillMaxSize()
		)

		// Image vignette
		DreamContentVignette()
	}

	// Overlay
	Row(
		modifier = Modifier
			.align(Alignment.BottomStart)
			.overscan(),
	) {
		SubcomposeAsyncImage(
			model = content.logoUrl,
			contentDescription = content.item.name,
			modifier = Modifier.height(75.dp),
			error = {
				Text(
					text = content.item.name.orEmpty(),
					style = TextStyle(
						color = Color.White,
						fontSize = 32.sp
					),
				)
			}
		)
	}
}
