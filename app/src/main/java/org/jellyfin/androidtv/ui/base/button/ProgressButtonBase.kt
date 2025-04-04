package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import org.jellyfin.androidtv.R

@Composable
fun ProgressButtonBase(
	progress: Float,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	enabled: Boolean = true,
	shape: Shape = ButtonDefaults.Shape,
	colors: ButtonColors = ButtonDefaults.colors(),
	interactionSource: MutableInteractionSource? = null,
	content: @Composable BoxScope.() -> Unit
) {
	val progressColor = colorResource(R.color.button_default_progress_background)

	ButtonBase(
		onClick = onClick,
		modifier = modifier,
		onLongClick = onLongClick,
		enabled = enabled,
		shape = shape,
		colors = colors,
		interactionSource = interactionSource,
	) {
		Box(Modifier.matchParentSize()) {
			Box(
				Modifier
					.fillMaxHeight()
					.fillMaxWidth(progress)
					.background(progressColor)
			)
		}

		content()
	}
}
