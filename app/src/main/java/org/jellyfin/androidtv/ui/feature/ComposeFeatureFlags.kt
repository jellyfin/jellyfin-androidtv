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
	const val ENABLE_COMPOSE_HOME = false // Disabled due to focus navigation issues

	/**
	 * Enable Compose-based browse screens
	 * When true, uses MediaBrowseLayout for library browsing
	 */
	const val ENABLE_COMPOSE_BROWSE = false // Start with false, enable when ready

	/**
	 * Enable Compose-based Movies library screen
	 * When true, uses ComposeMoviesFragment instead of BrowseGridFragment for Movies
	 */
	const val ENABLE_COMPOSE_MOVIES = true // Enable for testing

	/**
	 * Enable Compose-based TV Shows library screen
	 * When true, uses ComposeTvShowsFragment instead of BrowseGridFragment for TV Shows
	 */
	const val ENABLE_COMPOSE_TVSHOWS = true // Enable for testing

	/**
	 * Enable Compose-based Music library screen
	 * When true, uses ComposeMusicFragment instead of BrowseViewFragment for Music
	 */
	const val ENABLE_COMPOSE_MUSIC = true // Enable for testing

	/**
	 * Enable Compose-based Live TV library screen
	 * When true, uses ComposeLiveTvFragment instead of BrowseViewFragment for Live TV
	 */
	const val ENABLE_COMPOSE_LIVETV = true // Enable for testing

	/**
	 * Enable Compose-based grid layouts
	 * When true, uses MediaGrid instead of HorizontalGridPresenter
	 */
	const val ENABLE_COMPOSE_GRIDS = false

	/**
	 * Enable Compose-based detail screens
	 * When true, uses Compose for item detail layouts
	 */
	const val ENABLE_COMPOSE_DETAILS = false

	/**
	 * Enable enhanced Material 3 theming
	 * When true, uses JellyfinTvTheme with dynamic colors
	 */
	const val ENABLE_MATERIAL3_THEMING = true

	/**
	 * Enable debug logging for Compose components
	 */
	const val DEBUG_COMPOSE = true
}
