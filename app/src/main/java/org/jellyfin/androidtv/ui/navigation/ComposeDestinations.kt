package org.jellyfin.androidtv.ui.navigation

/**
 * Destination definitions for Compose-based fragments.
 * These destinations work alongside traditional Leanback fragments during migration.
 */
object ComposeDestinations {

	/**
	 * Constants for Compose fragment identification
	 */
	const val COMPOSE_HOME_SCREEN = "compose_home_screen"
	const val COMPOSE_TEST_SCREEN = "compose_test_screen"
	const val COMPOSE_ENHANCED_BROWSE = "compose_enhanced_browse"

	/**
	 * Gets the fragment class name for the Compose home screen
	 */
	fun getComposeHomeScreenClassName(): String {
		return "org.jellyfin.androidtv.ui.home.compose.SimpleHomeFragment"
	}

	/**
	 * Gets the fragment class name for the Compose test screen
	 */
	fun getComposeTestScreenClassName(): String {
		return "org.jellyfin.androidtv.ui.composable.test.ComposeTestScreenFragment"
	}

	/**
	 * Gets the fragment class name for the Compose enhanced browse screen
	 */
	fun getComposeEnhancedBrowseClassName(): String {
		return "org.jellyfin.androidtv.ui.browsing.compose.ComposeEnhancedBrowseFragment"
	}
}
