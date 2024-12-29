package org.jellyfin.androidtv.ui.base.button

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
	shape: Shape = CircleShape,
	contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
	interactionSource: MutableInteractionSource? = null,
	content: @Composable RowScope.() -> Unit
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

	Box(
		modifier = modifier
			.combinedClickable(
				interactionSource = interactionSource,
				indication = LocalIndication.current,
				enabled = enabled,
				role = Role.Button,
				onClick = onClick,
				onLongClick = onLongClick,
			)
			.background(colors.first, shape)
			.graphicsLayer {
				this.shape = shape
				this.clip = true
			}
	) {
		ProvideTextStyle(value = JellyfinTheme.typography.default.copy(fontSize = 14.sp, color = colors.second)) {
			Row(
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
		}
	}
}
