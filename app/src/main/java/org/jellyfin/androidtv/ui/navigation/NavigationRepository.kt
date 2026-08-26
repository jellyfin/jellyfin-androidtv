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
	 * Whether destinations exist below the current one. Unlike [canGoBack] this is false when the app
	 * was opened directly into a single screen, which is the state a freshly created activity should
	 * keep rather than reset.
	 */
	val hasBackStack: Boolean

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
	private val defaultDestination: Destination.Fragment,
) : NavigationRepository {
	/**
	 * The destinations currently on screen, deepest last. The top of the stack is what the user is
	 * looking at, mirroring the history kept by the view that renders the fragments. Keeping both in
	 * step matters: [canGoBack] gates the back handler, and a handler that is disabled hands the key
	 * press to the activity, which finishes it and closes the app.
	 */
	private val fragmentHistory = Stack<Destination.Fragment>().apply { push(defaultDestination) }

	private val _currentAction = MutableSharedFlow<NavigationAction>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
	override val currentAction = _currentAction.asSharedFlow()

	/**
	 * Whether the visible destination is the one the app starts on. Compared by fragment type because
	 * [Destination.Fragment] carries a [android.os.Bundle], which does not implement equality.
	 */
	private val isAtDefaultDestination: Boolean
		get() = fragmentHistory.size == 1 && fragmentHistory.peek().fragment == defaultDestination.fragment

	override fun navigate(destination: Destination, replace: Boolean) {
		Timber.i("Navigating to $destination (via navigate function)")
		val action = when (destination) {
			is Destination.Fragment -> NavigationAction.NavigateFragment(destination, true, replace, false)
		}
		if (destination is Destination.Fragment) {
			if (replace && fragmentHistory.isNotEmpty()) fragmentHistory[fragmentHistory.lastIndex] = destination
			else fragmentHistory.push(destination)
		}
		_currentAction.tryEmit(action)
	}

	override val canGoBack: Boolean get() = !isAtDefaultDestination

	override val hasBackStack: Boolean get() = fragmentHistory.size > 1

	override fun goBack(): Boolean {
		if (fragmentHistory.size > 1) {
			Timber.i("Navigating back")
			fragmentHistory.pop()
			_currentAction.tryEmit(NavigationAction.GoBack)
			return true
		}

		// The app was opened straight into a screen below the root, for example through a launcher
		// tile, a deep link or a search result. There is nothing to pop, but closing the app would be
		// wrong while a detail screen is on display: go to the default destination instead.
		if (!isAtDefaultDestination) {
			Timber.i("Navigating back to the default destination")
			reset(defaultDestination, clearHistory = true)
			return true
		}

		return false
	}

	override fun reset(destination: Destination.Fragment?, clearHistory: Boolean) {
		fragmentHistory.clear()
		val actualDestination = destination ?: defaultDestination
		fragmentHistory.push(actualDestination)
		_currentAction.tryEmit(NavigationAction.NavigateFragment(actualDestination, true, false, clearHistory))
		Timber.i("Navigating to $actualDestination (via reset, clearHistory=$clearHistory)")
	}
}
