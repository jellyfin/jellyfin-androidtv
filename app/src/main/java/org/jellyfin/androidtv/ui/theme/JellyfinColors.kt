package org.jellyfin.androidtv.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

// Jellyfin brand colors
object JellyfinColors {
    val Primary = Color(0xFF00A4DC) // Jellyfin blue
    val PrimaryVariant = Color(0xFF0078A8)
    val Secondary = Color(0xFF52B54B) // Jellyfin green
    val SecondaryVariant = Color(0xFF3A8A35)
    
    // Expressive colors for dynamic theming
    val Accent1 = Color(0xFFFF6B35) // Orange
    val Accent2 = Color(0xFF9B59B6) // Purple
    val Accent3 = Color(0xFFE74C3C) // Red
    
    // TV-optimized neutral colors
    val Surface = Color(0xFF101010)
    val SurfaceVariant = Color(0xFF1A1A1A)
    val Background = Color(0xFF0A0A0A)
    val OnSurface = Color(0xFFE0E0E0)
    val OnSurfaceVariant = Color(0xFFB0B0B0)
    val OnBackground = Color(0xFFFFFFFF)
    
    // Focus states for TV
    val Focused = Color(0xFFFFFFFF)
    val FocusedContainer = Color(0x33FFFFFF)
    val Selected = Color(0xFF00A4DC)
    val SelectedContainer = Color(0x3300A4DC)
    
    // TV-specific gradient colors for media cards
    val MediaCardGradientStart = Color(0x80000000)
    val MediaCardGradientEnd = Color(0x00000000)
}

/**
 * TV-specific color scheme extensions for Jetpack Compose TV
 */
@Immutable
data class JellyfinTvColorExtensions(
    val focused: Color = JellyfinColors.Focused,
    val focusedContainer: Color = JellyfinColors.FocusedContainer,
    val selected: Color = JellyfinColors.Selected,
    val selectedContainer: Color = JellyfinColors.SelectedContainer,
    val mediaControls: Color = Color(0xCC000000),
    val onMediaControls: Color = Color.White,
    val cardOverlay: Color = Color(0x80000000),
    val progressBarTrack: Color = Color(0x33FFFFFF),
    val progressBarProgress: Color = JellyfinColors.Primary,
)

/**
 * Material 3 Dark Color Scheme optimized for TV viewing
 */
val JellyfinDarkColorScheme = darkColorScheme(
    primary = JellyfinColors.Primary,
    onPrimary = Color.White,
    primaryContainer = JellyfinColors.PrimaryVariant,
    onPrimaryContainer = Color.White,
    
    secondary = JellyfinColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = JellyfinColors.SecondaryVariant,
    onSecondaryContainer = Color.White,
    
    tertiary = JellyfinColors.Accent1,
    onTertiary = Color.White,
    tertiaryContainer = Color(0x33FF6B35),
    onTertiaryContainer = Color.White,
    
    background = JellyfinColors.Background,
    onBackground = JellyfinColors.OnBackground,
    
    surface = JellyfinColors.Surface,
    onSurface = JellyfinColors.OnSurface,
    surfaceVariant = JellyfinColors.SurfaceVariant,
    onSurfaceVariant = JellyfinColors.OnSurfaceVariant,
    
    error = Color(0xFFFF5722),
    onError = Color.White,
    errorContainer = Color(0x33FF5722),
    onErrorContainer = Color.White,
    
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF2A2A2A),
    
    scrim = Color(0x80000000),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = JellyfinColors.PrimaryVariant,
    
    surfaceTint = JellyfinColors.Primary,
)

/**
 * Material 3 Light Color Scheme (for day/light theme variant)
 */
val JellyfinLightColorScheme = lightColorScheme(
    primary = JellyfinColors.PrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FF),
    onPrimaryContainer = Color(0xFF001F2A),
    
    secondary = JellyfinColors.SecondaryVariant,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E8),
    onSecondaryContainer = Color(0xFF1B5E20),
    
    tertiary = Color(0xFFE65100),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF3E2723),
    
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF121212),
    
    surface = Color.White,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF424242),
    
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),
    
    scrim = Color(0x80000000),
    inverseSurface = Color(0xFF121212),
    inverseOnSurface = Color(0xFFE0E0E0),
    inversePrimary = JellyfinColors.Primary,
    
    surfaceTint = JellyfinColors.Primary,
)

/**
 * Extended color scheme for TV-specific interaction states
 */
data class JellyfinTvColorScheme(
    val material3: ColorScheme,
    val tvExtensions: JellyfinTvColorExtensions = JellyfinTvColorExtensions(),
) {
    companion object {
        val Dark = JellyfinTvColorScheme(
            material3 = JellyfinDarkColorScheme
        )
        
        val Light = JellyfinTvColorScheme(
            material3 = JellyfinLightColorScheme,
            tvExtensions = JellyfinTvColorExtensions(
                focused = Color(0xFF121212),
                focusedContainer = Color(0x33121212),
                selected = JellyfinColors.PrimaryVariant,
                selectedContainer = Color(0x3300A4DC),
            )
        )
    }
}

/**
 * TV-optimized Material 3 Theme
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun JellyfinTvTheme(
    useDarkTheme: Boolean = true,
    colorScheme: JellyfinTvColorScheme = if (useDarkTheme) JellyfinTvColorScheme.Dark else JellyfinTvColorScheme.Light,
    content: @Composable () -> Unit
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
            onBackground = colorScheme.material3.onBackground,            surface = colorScheme.material3.surface,
            onSurface = colorScheme.material3.onSurface,
            surfaceVariant = colorScheme.material3.surfaceVariant,
            onSurfaceVariant = colorScheme.material3.onSurfaceVariant,
            error = colorScheme.material3.error,
            onError = colorScheme.material3.onError,
            errorContainer = colorScheme.material3.errorContainer,
            onErrorContainer = colorScheme.material3.onErrorContainer,
            border = colorScheme.material3.outline,
            borderVariant = colorScheme.material3.outlineVariant,
            scrim = colorScheme.material3.scrim,
            inverseSurface = colorScheme.material3.inverseSurface,
            inverseOnSurface = colorScheme.material3.inverseOnSurface,
            inversePrimary = colorScheme.material3.inversePrimary,
        ),
        content = content
    )
}
