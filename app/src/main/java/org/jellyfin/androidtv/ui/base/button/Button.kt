package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme

object ButtonDefaults {
	val Shape: Shape = CircleShape
	val ContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp)

	@ReadOnlyComposable
	@Composable
	fun colors(
		containerColor: Color = JellyfinTheme.colorScheme.button,
		contentColor: Color = JellyfinTheme.colorScheme.onButton,
		focusedContainerColor: Color = JellyfinTheme.colorScheme.buttonFocused,
		focusedContentColor: Color = JellyfinTheme.colorScheme.onButtonFocused,
		disabledContainerColor: Color = JellyfinTheme.colorScheme.buttonDisabled,
		disabledContentColor: Color = JellyfinTheme.colorScheme.onButtonDisabled,
	) = ButtonColors(
		containerColor = containerColor,
		contentColor = contentColor,
		focusedContainerColor = focusedContainerColor,
		focusedContentColor = focusedContentColor,
		disabledContainerColor = disabledContainerColor,
		disabledContentColor = disabledContentColor,
	)
}

@Composable
fun Button(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	enabled: Boolean = true,
	shape: Shape = ButtonDefaults.Shape,
	colors: ButtonColors = ButtonDefaults.colors(),
	contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
	interactionSource: MutableInteractionSource? = null,
	content: @Composable RowScope.() -> Unit
) {
	ButtonBase(
		onClick = onClick,
		modifier = modifier,
		onLongClick = onLongClick,
		enabled = enabled,
		shape = shape,
		colors = colors,
		interactionSource = interactionSource,
	) {
		ButtonRow(
			contentPadding = contentPadding,
			content = content,
		)
	}
}

@Composable
fun ProgressButton(
	progress: Float,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	enabled: Boolean = true,
	shape: Shape = ButtonDefaults.Shape,
	colors: ButtonColors = ButtonDefaults.colors(),
	contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
	interactionSource: MutableInteractionSource? = null,
	content: @Composable RowScope.() -> Unit
) {
	ProgressButtonBase(
		progress = progress,
		onClick = onClick,
		modifier = modifier,
		onLongClick = onLongClick,
		enabled = enabled,
		shape = shape,
		colors = colors,
		interactionSource = interactionSource,
	) {
		ButtonRow(
			contentPadding = contentPadding,
			content = content,
		)
	}
}

@Composable
private fun ButtonRow(
	contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
	content: @Composable RowScope.() -> Unit,
) = Row(
	modifier = Modifier
		.defaultMinSize(
			minWidth = 58.dp,
			minHeight = 40.dp
		)
		.padding(contentPadding),
	horizontalArrangement = Arrangement.Center,
	verticalAlignment = Alignment.CenterVertically,
	content = content
)
