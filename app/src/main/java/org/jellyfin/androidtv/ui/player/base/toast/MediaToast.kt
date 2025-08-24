package org.jellyfin.androidtv.ui.player.base.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

data class MediaToastColors(
	val backgroundColor: Color,
	val iconColor: Color,
	val progressBackgroundColor: Color,
	val progressFillColor: Color,
)

object MediaToastDefaults {
	@ReadOnlyComposable
	@Composable
	fun colors(
		backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
		iconColor: Color = Color.White,
		progressBackgroundColor: Color = Color.Black,
		progressFillColor: Color = JellyfinTheme.colorScheme.rangeControlFill,
	) = MediaToastColors(
		backgroundColor = backgroundColor,
		iconColor = iconColor,
		progressBackgroundColor = progressBackgroundColor,
		progressFillColor = progressFillColor,
	)

}

@Composable
fun MediaToast(
	visible: Boolean,
	icon: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	progress: Float? = null,
	colors: MediaToastColors = MediaToastDefaults.colors(),
) {
	AnimatedVisibility(
		visible = visible,
		enter = fadeIn() + scaleIn(initialScale = 0.8f),
		exit = fadeOut() + scaleOut(targetScale = 1.2f),
		modifier = modifier
			.fillMaxSize()
			.wrapContentSize(Alignment.Center)
	) {
		Box(
			modifier = Modifier
				.size(96.dp)
				.drawWithContent {
					// Toast background
					drawCircle(colors.backgroundColor)

					// Toast icon
					drawContent()

					// Toast progress
					if (progress != null) {
						val width = 4.dp.toPx()

						// Background
						drawCircle(
							style = Stroke(width),
							color = colors.progressBackgroundColor,
							alpha = 0.4f,
						)

						// Foreground
						drawArc(
							style = Stroke(width, cap = StrokeCap.Round),
							color = colors.progressFillColor,
							useCenter = false,
							startAngle = -90f,
							sweepAngle = 360f * progress,
						)
					}
				}
				.padding(16.dp),
			contentAlignment = Alignment.Center,
		) {
			ProvideTextStyle(LocalTextStyle.current.copy(color = colors.iconColor)) {
				icon()
			}
		}
	}
}
