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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
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
	interactionSource: MutableInteractionSource? = null,
	content: @Composable BoxScope.() -> Unit
) {
	@Suppress("NAME_SHADOWING")
	val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val pressed by interactionSource.collectIsPressedAsState()

	val containerColor = colorResource(R.color.button_default_normal_background)
	val contentColor = colorResource(R.color.button_default_normal_text)
	val focusedContainerColor = colorResource(R.color.button_default_highlight_background)
	val focusedContentColor = colorResource(R.color.button_default_highlight_text)
	val disabledContainerColor = colorResource(R.color.button_default_disabled_background)
	val disabledContentColor = colorResource(R.color.button_default_disabled_text)

	val colors = when {
		!enabled -> disabledContainerColor to disabledContentColor
		pressed -> focusedContainerColor to focusedContentColor
		focused -> focusedContainerColor to focusedContentColor
		else -> containerColor to contentColor
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
