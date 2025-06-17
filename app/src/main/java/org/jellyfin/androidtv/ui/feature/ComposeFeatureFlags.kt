package org.jellyfin.androidtv.ui.feature

/**
 * Feature flags for gradual Compose migration
 * This allows you to test and enable new features incrementally
 */
object ComposeFeatureFlags {
    
    /**
     * Enable the new Compose-based home screen
     * When true, uses MediaBrowseLayout instead of Leanback RowsSupportFragment
     */
    val ENABLE_COMPOSE_HOME = true // Set to false to disable
    
    /**
     * Enable Compose-based browse screens
     * When true, uses MediaBrowseLayout for library browsing
     */
    val ENABLE_COMPOSE_BROWSE = false // Start with false, enable when ready
    
    /**
     * Enable Compose-based grid layouts
     * When true, uses MediaGrid instead of HorizontalGridPresenter
     */
    val ENABLE_COMPOSE_GRIDS = false
    
    /**
     * Enable Compose-based detail screens
     * When true, uses Compose for item detail layouts
     */
    val ENABLE_COMPOSE_DETAILS = false
    
    /**
     * Enable enhanced Material 3 theming
     * When true, uses JellyfinTvTheme with dynamic colors
     */
    val ENABLE_MATERIAL3_THEMING = true
    
    /**
     * Enable debug logging for Compose components
     */
    val DEBUG_COMPOSE = true
}
