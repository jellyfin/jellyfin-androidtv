package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun JellyfinTheme(
	colorScheme: ColorScheme = JellyfinTheme.colorScheme,
	shapes: Shapes = JellyfinTheme.shapes,
	typography: Typography = JellyfinTheme.typography,
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(
		LocalColorScheme provides colorScheme,
		LocalShapes provides shapes,
		LocalTypography provides typography,
		// Disable overscroll
		LocalOverscrollFactory provides null,
	) {
		ProvideTextStyle(value = typography.default, content = content)
	}
}

object JellyfinTheme {
	val colorScheme: ColorScheme
		@Composable @ReadOnlyComposable get() = LocalColorScheme.current

	val typography: Typography
		@Composable @ReadOnlyComposable get() = LocalTypography.current

	val shapes: Shapes
		@Composable @ReadOnlyComposable get() = LocalShapes.current
}
