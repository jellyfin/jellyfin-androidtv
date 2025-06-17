package org.jellyfin.androidtv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * Local provider for TV-specific color scheme extensions
 */
val LocalJellyfinTvColorScheme = staticCompositionLocalOf { JellyfinTvColorScheme.Dark }

/**
 * Jellyfin theme using Material 3 with TV optimizations and expressive theming
 * 
 * This theme provides:
 * - Material 3 color system with Jellyfin branding
 * - TV-optimized typography and shapes
 * - Extended TV color scheme for focus/selection states
 * - Support for both Material 3 and TV Material 3 components
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun JellyfinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> JellyfinTvColorScheme.Dark
        else -> JellyfinTvColorScheme.Light
    }
    
    CompositionLocalProvider(
        LocalJellyfinTvColorScheme provides colorScheme
    ) {
        // Provide both Material 3 and TV Material 3 themes
        MaterialTheme(
            colorScheme = colorScheme.material3,
            typography = JellyfinTvTypography,
            shapes = JellyfinTvShapes,
        ) {
            TvMaterialTheme(
                colorScheme = androidx.tv.material3.darkColorScheme(
                    primary = colorScheme.material3.primary,
                    onPrimary = colorScheme.material3.onPrimary,
                    primaryContainer = colorScheme.material3.primaryContainer,
                    onPrimaryContainer = colorScheme.material3.onPrimaryContainer,
                    secondary = colorScheme.material3.secondary,
                    onSecondary = colorScheme.material3.onSecondary,
                    secondaryContainer = colorScheme.material3.secondaryContainer,
                    onSecondaryContainer = colorScheme.material3.onSecondaryContainer,
                    tertiary = colorScheme.material3.tertiary,
                    onTertiary = colorScheme.material3.onTertiary,
                    tertiaryContainer = colorScheme.material3.tertiaryContainer,
                    onTertiaryContainer = colorScheme.material3.onTertiaryContainer,
                    background = colorScheme.material3.background,
                    onBackground = colorScheme.material3.onBackground,
                    surface = colorScheme.material3.surface,
                    onSurface = colorScheme.material3.onSurface,
                    surfaceVariant = colorScheme.material3.surfaceVariant,
                    onSurfaceVariant = colorScheme.material3.onSurfaceVariant,
                    error = colorScheme.material3.error,
                    onError = colorScheme.material3.onError,
                    errorContainer = colorScheme.material3.errorContainer,
                    onErrorContainer = colorScheme.material3.onErrorContainer,
                    border = colorScheme.material3.outline,
                ),
                shapes = androidx.tv.material3.Shapes(
                    extraSmall = JellyfinTvShapes.extraSmall,
                    small = JellyfinTvShapes.small,
                    medium = JellyfinTvShapes.medium,
                    large = JellyfinTvShapes.large,
                    extraLarge = JellyfinTvShapes.extraLarge,
                ),
                content = content
            )
        }
    }
}

/**
 * Access to the Jellyfin TV color scheme from within composables
 */
object JellyfinTheme {
    val tvColors: JellyfinTvColorScheme
        @Composable get() = LocalJellyfinTvColorScheme.current
}
