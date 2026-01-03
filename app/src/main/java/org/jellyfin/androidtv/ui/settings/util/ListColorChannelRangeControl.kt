package org.jellyfin.androidtv.ui.settings.util

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.form.RangeControlDefaults
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.design.Tokens
import kotlin.math.roundToInt

@Composable
fun ListColorChannelRangeControl(
	headingContent: @Composable () -> Unit,
	channel: Color,
	value: Color,
	onValueChange: (color: Color) -> Unit,
) {
	val interactionSource = remember { MutableInteractionSource() }

	val channelValue = when (channel) {
		Color.Red -> value.red
		Color.Green -> value.green
		Color.Blue -> value.blue
		Color.Transparent -> value.alpha
		else -> error("Unknown color channel $channel")
	}

	val backgroundStart = Color(
		red = if (channel == Color.Red) 0f else value.red,
		green = if (channel == Color.Green) 0f else value.green,
		blue = if (channel == Color.Blue) 0f else value.blue,
		alpha = if (channel == Color.Transparent) 0f else 1f,
	)
	val backgroundEnd = Color(
		red = if (channel == Color.Red) 1f else value.red,
		green = if (channel == Color.Green) 1f else value.green,
		blue = if (channel == Color.Blue) 1f else value.blue,
		alpha = 1f,
	)
	val backgroundBrush = Brush.horizontalGradient(listOf(backgroundStart, backgroundEnd))

	ListControl(
		headingContent = headingContent,
		interactionSource = interactionSource,
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
		) {
			RangeControl(
				modifier = Modifier
					.height(4.dp)
					.weight(1f)
					.background(backgroundBrush, RoundedCornerShape(2.dp)),
				interactionSource = interactionSource,
				min = 0f,
				max = 1f,
				stepForward = 0.01f,
				value = channelValue,
				onValueChange = {
					val newValue = Color(
						red = if (channel == Color.Red) it else value.red,
						green = if (channel == Color.Green) it else value.green,
						blue = if (channel == Color.Blue) it else value.blue,
						alpha = if (channel == Color.Transparent) it else value.alpha,
					)
					onValueChange(newValue)
				},
				colors = RangeControlDefaults.colors(
					backgroundColor = Color.Transparent,
					fillColor = Color.Transparent
				)
			)

			Spacer(Modifier.width(Tokens.Space.spaceSm))

			Box(
				modifier = Modifier.sizeIn(minWidth = 32.dp),
				contentAlignment = Alignment.CenterEnd
			) {
				Text("${(channelValue * 100f).roundToInt()}%")
			}
		}
	}
}
