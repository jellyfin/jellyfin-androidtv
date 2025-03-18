package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

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

@Composable
fun Button(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	enabled: Boolean = true,
	shape: Shape = ButtonDefaults.Shape,
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
		interactionSource = interactionSource,
	) {
		ButtonRow(
			contentPadding = contentPadding,
			content = content,
		)
	}
}
