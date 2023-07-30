package org.jellyfin.androidtv.ui.composable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun ZoomBox(
	initialValue: Float = 1f,
	targetValue: Float = 2f,
	delayMillis: Int = 0,
	durationMillis: Int = 1_000,
	content: @Composable BoxScope.() -> Unit,
) {
	val transition = rememberInfiniteTransition(
		label = "ZoomBoxTransition"
	)
	val scale by transition.animateFloat(
		initialValue = initialValue,
		targetValue = targetValue,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = durationMillis, delayMillis = delayMillis, LinearEasing),
			repeatMode = RepeatMode.Reverse
		),
		label = "ZoomBoxTransitionScale"
	)

	Box(
		modifier = Modifier.graphicsLayer {
			scaleX = scale
			scaleY = scale
		},
		content = content,
	)
}
