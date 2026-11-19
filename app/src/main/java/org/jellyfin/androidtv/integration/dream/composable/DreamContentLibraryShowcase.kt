package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
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
	Column(
		modifier = Modifier
			.align(Alignment.BottomStart)
			.overscan(),
	) {
		var hintVisible by remember(content.item.id) { mutableStateOf(true) }
		LaunchedEffect(content.item.id) {
			delay(8.seconds)
			hintVisible = false
		}
		AnimatedVisibility(
			visible = hintVisible,
			enter = fadeIn(),
			exit = fadeOut(),
		) {
			Text(
				text = stringResource(R.string.screensaver_open_item_hint),
				style = TextStyle(
					color = Color.White.copy(alpha = 0.7f),
					fontSize = 14.sp,
				),
				modifier = Modifier.padding(bottom = 8.dp),
			)
		}

		if (content.logo != null) {
			BoxWithConstraints {
				Image(
					bitmap = content.logo.asImageBitmap(),
					contentDescription = content.item.name,
					modifier = Modifier
						.sizeIn(
							maxWidth = maxWidth * 0.35f,
							maxHeight = 75.dp
						)
				)
			}
		} else {
			Text(
				text = content.item.name.orEmpty(),
				style = TextStyle(
					color = Color.White,
					fontSize = 32.sp
				),
			)
		}
	}
}
