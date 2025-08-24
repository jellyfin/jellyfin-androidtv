package org.jellyfin.androidtv.ui.base.popover

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jellyfin.androidtv.ui.base.JellyfinTheme

object PopoverDefaults {
	val Shape: Shape = RoundedCornerShape(4.dp)
}

@Composable
fun Popover(
	expanded: Boolean,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	alignment: Alignment = Alignment.TopStart,
	offset: DpOffset = DpOffset.Zero,
	shape: Shape = PopoverDefaults.Shape,
	backgroundColor: Color = JellyfinTheme.colorScheme.popover,
	content: @Composable BoxScope.() -> Unit,
) {
	val density = LocalDensity.current
	val focusRequester = remember { FocusRequester() }
	val popupPositionProvider = remember(alignment, density, offset) {
		PopoverMenuPositionProvider(
			alignment = alignment,
			offset = IntOffset(
				x = with(density) { offset.x.roundToPx() },
				y = with(density) { offset.y.roundToPx() },
			)
		)
	}

	val transition = updateTransition(expanded)
	val alpha by transition.animateFloat(
		targetValueByState = { expanded -> if (expanded) 1f else 0f }
	)

	if (alpha != 0f) {
		Popup(
			onDismissRequest = onDismissRequest,
			properties = PopupProperties(
				focusable = true,
				dismissOnBackPress = true,
				dismissOnClickOutside = true,
			),
			popupPositionProvider = popupPositionProvider
		) {
			Box(
				modifier = modifier
					.graphicsLayer(
						shape = shape,
						clip = true,
						shadowElevation = with(density) { 4.dp.toPx() },
						ambientShadowColor = Color.Black.copy(alpha = alpha),
						spotShadowColor = Color.Black.copy(alpha = alpha),
					)
					.graphicsLayer(
						shape = shape,
						clip = true,
						alpha = alpha,
					)
					.background(backgroundColor, shape)
					.wrapContentSize()
					.focusRequester(focusRequester)
					.focusGroup()
			) {
				content()
			}
		}

		LaunchedEffect(focusRequester) {
			focusRequester.requestFocus()
		}
	}
}
