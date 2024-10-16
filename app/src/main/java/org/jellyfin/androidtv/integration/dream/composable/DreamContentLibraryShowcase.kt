package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
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
		Column {
			if (content.logo != null) {
				Image(
					bitmap = content.logo.asImageBitmap(),
					contentDescription = content.item.name,
					modifier = Modifier
						.height(100.dp)
						.widthIn(max = 300.dp)
						.fillMaxSize()
						.fillMaxWidth()
				)
			} else {
				Text(
					text = content.item.name.orEmpty().uppercase(),
					style = TextStyle(
						color = Color.White,
						fontSize = 32.sp
					),
				)
			}
			Text(
				text = content.item.productionYear.toString(),
				style = TextStyle(
					color = Color.White,
					fontSize = 24.sp
				),
			)
		}
	}
}
