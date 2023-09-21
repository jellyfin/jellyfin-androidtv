package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.ui.composable.ZoomBox
import org.jellyfin.androidtv.ui.composable.overscan

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
		Image(
			bitmap = content.backdrop.asImageBitmap(),
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
		Text(
			text = content.item.name.orEmpty(),
			style = TextStyle(
				color = Color.White,
				fontSize = 32.sp
			),
		)
	}
}
