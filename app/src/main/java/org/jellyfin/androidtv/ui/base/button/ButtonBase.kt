package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ButtonBase(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	enabled: Boolean = true,
	shape: Shape = ButtonDefaults.Shape,
	colors: ButtonColors = ButtonDefaults.colors(),
	interactionSource: MutableInteractionSource? = null,
	content: @Composable BoxScope.() -> Unit
) {
	@Suppress("NAME_SHADOWING")
	val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val pressed by interactionSource.collectIsPressedAsState()

	val colors = when {
		!enabled -> colors.disabledContainerColor to colors.disabledContentColor
		pressed -> colors.focusedContainerColor to colors.focusedContentColor
		focused -> colors.focusedContainerColor to colors.focusedContentColor
		else -> colors.containerColor to colors.contentColor
	}

	ProvideTextStyle(value = JellyfinTheme.typography.default.copy(fontSize = 14.sp, color = colors.second)) {
		Box(
			modifier = modifier
				.combinedClickable(
					interactionSource = interactionSource,
					indication = null,
					enabled = enabled,
					role = Role.Button,
					onClick = onClick,
					onLongClick = onLongClick,
				)
				.background(colors.first, shape)
				.clip(shape),
			content = content,
		)
	}
}
