package org.jellyfin.androidtv.ui.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.util.Stack

/**
 * Repository for app navigation. This manages the screens/pages for the app.
 */
interface NavigationRepository {
	/**
	 * The current action to act on.
	 *
	 * @see NavigationAction
	 */
	val currentAction: SharedFlow<NavigationAction>

	/**
	 * Navigate to [destination].
	 *
	 * @see Destinations
	 */
	fun navigate(destination: Destination) = navigate(destination, false)

	/**
	 * Navigate to [destination].
	 *
	 * @see Destinations
	 */
	fun navigate(destination: Destination, replace: Boolean)

	/**
	 * Whether the [goBack] function will succeed or not.
	 *
	 * @see [goBack]
	 */
	val canGoBack: Boolean

	/**
	 * Go back to the previous fragment. The back stack does not consider other destination types.
	 *
	 * @see [canGoBack]
	 */
	fun goBack(): Boolean

	/**
	 * Reset navigation to the initial destination or a specific [Destination.Fragment].
	 *
	 * @param clearHistory Empty out the back stack
	 */
	fun reset(destination: Destination.Fragment? = null, clearHistory: Boolean)

	/**
	 * Reset navigation to the initial destination or a specific [Destination.Fragment] without clearing history.
	 */
	fun reset(destination: Destination.Fragment? = null) = reset(destination, false)
}

class NavigationRepositoryImpl(
	private val initialDestination: Destination.Fragment,
) : NavigationRepository {
	private val fragmentHistory = Stack<Destination.Fragment>()

	private val _currentAction = MutableSharedFlow<NavigationAction>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
	override val currentAction = _currentAction.asSharedFlow()

	init {
		// Never add the initial destination to the history to prevent an empty screen when the user
		// uses the "back" button to close the app
		_currentAction.tryEmit(NavigationAction.NavigateFragment(initialDestination, false, false, false))
		Timber.d("Navigating to $initialDestination (via init)")
	}

	override fun navigate(destination: Destination, replace: Boolean) {
		Timber.d("Navigating to $destination (via navigate function)")
		val action = when (destination) {
			is Destination.Fragment -> NavigationAction.NavigateFragment(destination, true, replace, false)
			is Destination.Activity -> NavigationAction.NavigateActivity(destination) {
				Timber.d("Navigating to nothing")
				_currentAction.tryEmit(NavigationAction.Nothing)
			}
		}
		_currentAction.tryEmit(action)
		if (destination is Destination.Fragment) {
			if (replace && fragmentHistory.isNotEmpty()) fragmentHistory[fragmentHistory.lastIndex] = destination
			else fragmentHistory.push(destination)
		}
	}

	override val canGoBack: Boolean get() = fragmentHistory.isNotEmpty()

	override fun goBack(): Boolean {
		if (fragmentHistory.empty()) return false

		Timber.d("Navigating back")
		fragmentHistory.pop()
		_currentAction.tryEmit(NavigationAction.GoBack)
		return true
	}

	override fun reset(destination: Destination.Fragment?, clearHistory: Boolean) {
		fragmentHistory.clear()
		val actualDestination = destination ?: initialDestination
		_currentAction.tryEmit(NavigationAction.NavigateFragment(actualDestination, false, false, clearHistory))
		Timber.d("Navigating to $actualDestination (via reset, clearHistory=$clearHistory)")
	}
}

