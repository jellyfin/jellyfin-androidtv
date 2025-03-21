package org.jellyfin.androidtv.ui.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.sdk.model.api.LyricDto
import org.jellyfin.sdk.model.extensions.ticks
import kotlin.time.Duration

data class TimedLine(
	val timestamp: Duration,
	val text: String,
)

private data class LyricsBoxContentMeasurements(
	val size: Size,
	val items: List<Measured>,
)

private fun Collection<TimedLine>.indexAtTimestamp(timestamp: Duration) = indexOfLast { it.timestamp <= timestamp }.takeIf { it != -1 }

@Composable
private fun LyricsLine(
	text: String,
	fontSize: TextUnit,
	color: Color,
	active: Boolean = false,
	gap: Dp = 15.dp,
) {
	val color by animateColorAsState(
		targetValue = if (active) color else color.copy(alpha = 0.5f),
		animationSpec = tween(),
		label = "LyricsLineColor",
	)

	val scale by animateFloatAsState(
		targetValue = if (active) 1.1f else 1f,
		animationSpec = tween(),
		label = "LyricsLineScale",
	)

	Text(
		text = text,
		textAlign = TextAlign.Center,
		fontSize = fontSize,
		color = color,
		modifier = Modifier
			.padding(bottom = gap)
			.scale(scale)
	)
}

@Composable
private fun <T> LyricsBoxContent(
	items: Collection<T>,
	modifier: Modifier = Modifier,
	onMeasured: ((measurements: LyricsBoxContentMeasurements) -> Unit)? = null,
	itemContent: @Composable (item: T, index: Int) -> Unit,
) = Layout(
	modifier = modifier,
	measurePolicy = { measurables, constraints ->
		val placeables = measurables.map { measurable ->
			measurable.measure(constraints.copy(minWidth = constraints.maxWidth))
		}
		if (onMeasured != null) {
			val totalHeight = placeables.sumOf { it.height }.toFloat()
			onMeasured(LyricsBoxContentMeasurements(Size(1f, totalHeight), placeables))
		}

		layout(constraints.maxWidth, constraints.maxHeight) {
			// Start at the offset until the active line and half the height of this container to vertically center the item
			var yOffset = constraints.maxHeight / 2

			for (placeable in placeables) {
				placeable.placeRelative(
					x = 0,
					y = yOffset,
				)

				yOffset += placeable.height
			}
		}
	},

	content = {
		items.forEachIndexed { index, item ->
			itemContent(item, index)
		}
	},
)

@Composable
fun LyricsBox(
	lines: List<TimedLine>,
	currentTimestamp: Duration = Duration.ZERO,
	fontSize: TextUnit = LocalTextStyle.current.fontSize,
	color: Color = LocalTextStyle.current.color,
) {
	var lineMeasurements by remember { mutableStateOf<List<Measured>>(emptyList()) }
	val activeLine = lines.indexAtTimestamp(currentTimestamp)
	val activeLineOffsetAnimation = remember { Animatable(0f) }

	LaunchedEffect(activeLine) {
		var offset = 0f

		if (activeLine != null) {
			// Add offset for all previous items
			offset += lineMeasurements.take(activeLine).sumOf { it.measuredHeight }
			// Add offset for half-height to center item
			offset += lineMeasurements.getOrNull(activeLine)?.measuredHeight?.div(2) ?: 0
		}

		activeLineOffsetAnimation.animateTo(offset)
	}

	LyricsBoxContent(
		items = lines,
		modifier = Modifier.graphicsLayer {
			translationY = -activeLineOffsetAnimation.value
		},
		onMeasured = { measurements -> lineMeasurements = measurements.items }
	) { line, index ->
		LyricsLine(
			text = line.text,
			active = index == activeLine,
			fontSize = fontSize,
			color = color,
		)
	}
}

@Composable
fun LyricsBox(
	lines: List<String>,
	modifier: Modifier = Modifier,
	currentTimestamp: Duration = Duration.ZERO,
	duration: Duration = Duration.ZERO,
	paused: Boolean = false,
	fontSize: TextUnit = LocalTextStyle.current.fontSize,
	color: Color = LocalTextStyle.current.color,
) = Box(modifier) {
	var totalHeight by remember { mutableFloatStateOf(0f) }
	val progress = rememberPlayerProgress(!paused, currentTimestamp, duration)

	LyricsBoxContent(
		items = lines,
		modifier = Modifier.graphicsLayer {
			translationY = -(progress * totalHeight)
		},
		onMeasured = { measurements -> totalHeight = measurements.size.height }
	) { line, index ->
		LyricsLine(
			text = line,
			fontSize = fontSize,
			color = color,
		)
	}
}

@Composable
fun LyricsDtoBox(
	lyricDto: LyricDto,
	modifier: Modifier = Modifier,
	currentTimestamp: Duration = Duration.ZERO,
	duration: Duration = Duration.ZERO,
	paused: Boolean = false,
	fontSize: TextUnit = LocalTextStyle.current.fontSize,
	color: Color = LocalTextStyle.current.color,
) = Box(modifier) {
	val lyrics = lyricDto.lyrics
	val isTimed = lyrics.firstOrNull()?.start != null
	if (isTimed) {
		LyricsBox(
			lines = lyrics.map {
				TimedLine(it.start?.ticks ?: Duration.ZERO, it.text)
			},
			currentTimestamp = currentTimestamp,
			fontSize = fontSize,
			color = color,
		)
	} else {
		LyricsBox(
			lines = lyrics.map { it.text },
			currentTimestamp = currentTimestamp,
			duration = duration,
			paused = paused,
			fontSize = fontSize,
			color = color,
		)
	}
}
