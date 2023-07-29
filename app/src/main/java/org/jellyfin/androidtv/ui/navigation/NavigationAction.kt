package org.jellyfin.androidtv.ui.navigation

/**
 * Emitted actions from the navigation repository.
 */
sealed interface NavigationAction {
	/**
	 * Navigate to the fragment in [destination].
	 */
	data class NavigateFragment(
		val destination: Destination.Fragment,
		val addToBackStack: Boolean,
		val replace: Boolean,
	) : NavigationAction

	/**
	 * Open the activity in [destination] and immediatly call [onOpened] to clear the emitted state.
	 */
	data class NavigateActivity(
		val destination: Destination.Activity,
		val onOpened: () -> Unit,
	) : NavigationAction

	/**
	 * Go back to the previous fragment manager state.
	 */
	data object GoBack : NavigationAction

	/**
	 * Do nothing.
	 */
	data object Nothing : NavigationAction
}
