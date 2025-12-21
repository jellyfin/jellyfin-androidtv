package org.jellyfin.androidtv.ui.base.form

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import org.jellyfin.androidtv.ui.base.JellyfinTheme

@Immutable
data class RangeControlColors(
	val backgroundColor: Color,
	val fillColor: Color,
	val knobColor: Color,
)

object RangeControlDefaults {
	@ReadOnlyComposable
	@Composable
	fun colors(
		backgroundColor: Color = JellyfinTheme.colorScheme.rangeControlBackground,
		fillColor: Color = JellyfinTheme.colorScheme.rangeControlFill,
		knobColor: Color = JellyfinTheme.colorScheme.rangeControlKnob,
	) = RangeControlColors(
		backgroundColor = backgroundColor,
		fillColor = fillColor,
		knobColor = knobColor,
	)
}

@Composable
fun RangeControl(
	modifier: Modifier = Modifier,
	interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
	min: Float = 0f,
	max: Float = 1f,
	value: Float = 0f,
	stepForward: Float = 0.01f,
	stepBackward: Float = stepForward,
	onValueChange: ((vakye: Float) -> Unit)? = null,
	enabled: Boolean = true,
	colors: RangeControlColors = RangeControlDefaults.colors(),
) {
	val focused by interactionSource.collectIsFocusedAsState()
	val knobSize by animateFloatAsState(if (focused) 1.75f else 1f)

	Box(
		modifier = modifier
			.onKeyEvent {
				if (!enabled) return@onKeyEvent false

				val isForward = it.key == Key.DirectionRight
				val isRewind = it.key == Key.DirectionLeft
				val isScrubbing = isForward || isRewind
				val isKeyDown = it.type == KeyEventType.KeyDown

				val newValue = when {
					isKeyDown && isForward -> (value + stepForward).coerceAtMost(max)
					isKeyDown && isRewind -> (value - stepBackward).coerceAtLeast(min)
					else -> value
				}

				if (value != newValue && onValueChange != null) onValueChange(newValue)

				return@onKeyEvent isScrubbing
			}
			.focusable(interactionSource = interactionSource, enabled = enabled)
			.drawWithContent {
				val barCornerRadius = CornerRadius(size.minDimension, size.minDimension)

				// Background bar
				drawRoundRect(
					color = colors.backgroundColor,
					cornerRadius = barCornerRadius,
				)

				// Get percental value based on min/max options
				val valuePercentage = (value - min) / (max - min)

				// Value fill bar
				if (valuePercentage > 0) {
					drawRoundRect(
						color = colors.fillColor,
						size = size.copy(
							width = valuePercentage * size.width,
						),
						cornerRadius = barCornerRadius,
					)
				}

				// Progress knob
				drawCircle(
					color = colors.knobColor,
					center = center.copy(
						x = valuePercentage * size.width,
					),
					radius = size.minDimension * knobSize,
				)
			}
	)
}
