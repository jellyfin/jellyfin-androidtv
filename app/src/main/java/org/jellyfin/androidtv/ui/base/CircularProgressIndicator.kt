package org.jellyfin.androidtv.ui.base

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun CircularProgressIndicator(
	color: Color = LocalTextStyle.current.color,
	modifier: Modifier = Modifier,
) {
	val infiniteTransition = rememberInfiniteTransition()
	val rotation by infiniteTransition.animateFloat(
		initialValue = 0f,
		targetValue = 360f,
		animationSpec = infiniteRepeatable(
			animation = tween(
				durationMillis = 1_500, // 1.5 seconds
				easing = LinearEasing,
			)
		)
	)

	Canvas(
		modifier = modifier
			.aspectRatio(1f)
			.rotate(rotation)
	) {
		drawArc(
			color = color,
			startAngle = 0f,
			sweepAngle = 270f,
			useCenter = false,
			style = Stroke(width = 4f, cap = StrokeCap.Round)
		)
	}
}
