package org.jellyfin.androidtv.ui.base.list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalShapes
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonColors

object ListButtonDefaults {
	@ReadOnlyComposable
	@Composable
	fun colors(
		containerColor: Color = JellyfinTheme.colorScheme.listButton,
		contentColor: Color = JellyfinTheme.colorScheme.onListButton,
		focusedContainerColor: Color = JellyfinTheme.colorScheme.listButtonFocused,
		focusedContentColor: Color = JellyfinTheme.colorScheme.onListButtonFocused,
		disabledContainerColor: Color = JellyfinTheme.colorScheme.listButtonDisabled,
		disabledContentColor: Color = JellyfinTheme.colorScheme.onListButtonDisabled,
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
fun ListButton(
	onClick: () -> Unit,
	headingContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	colors: ButtonColors = ListButtonDefaults.colors(),
	overlineContent: (@Composable () -> Unit)? = null,
	captionContent: (@Composable () -> Unit)? = null,
	leadingContent: (@Composable () -> Unit)? = null,
	trailingContent: (@Composable () -> Unit)? = null,
) {
	ButtonBase(
		onClick = onClick,
		colors = colors,
		enabled = enabled,
		shape = LocalShapes.current.large,
		modifier = modifier
			.fillMaxWidth()
	) {
		ListItemContent(
			headingContent = headingContent,
			overlineContent = overlineContent,
			captionContent = captionContent,
			leadingContent = leadingContent,
			trailingContent = trailingContent,
			headingStyle = JellyfinTheme.typography.listHeadline
				.copy(color = JellyfinTheme.colorScheme.listHeadline),
		)
	}
}
