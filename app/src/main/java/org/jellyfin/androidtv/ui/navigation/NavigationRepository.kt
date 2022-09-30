package org.jellyfin.androidtv.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Stack

interface NavigationRepository {
	val currentDestination: StateFlow<Destination>

	fun navigate(destination: Destination) = navigate(destination, NavigationOptions())
	fun navigate(destination: Destination, options: NavigationOptions)

	val canGoBack: Boolean
	fun goBack(): Boolean
}

class NavigationRepositoryImpl(
	initialDestination: Destination,
) : NavigationRepository {
	private val history = Stack<Destination>()

	private val _currentDestination = MutableStateFlow(initialDestination)
	override val currentDestination = _currentDestination.asStateFlow()

	override fun navigate(destination: Destination, options: NavigationOptions) {
		if (options.clearHistory) history.clear()

		if (!options.replaceCurrentDestination) history.push(_currentDestination.value)
		_currentDestination.value = destination
	}

	override val canGoBack: Boolean get() = history.isNotEmpty()

	override fun goBack(): Boolean {
		if (history.empty()) return false

		_currentDestination.value = history.pop()
		return true
	}
}

