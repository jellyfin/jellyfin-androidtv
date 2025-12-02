package org.jellyfin.androidtv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

class Router(
	val routes: Map<String, @Composable (() -> Unit)>,
	val backStack: SnapshotStateList<String>,
) {
	// Route resolving

	fun resolve(route: String): @Composable (() -> Unit)? = routes[route]
	fun verifyRoute(route: String) = require(resolve(route) != null) { "Invalid route $route" }

	// Route manipulation

	fun push(route: String) {
		verifyRoute(route)

		backStack.add(route)
	}

	fun replace(route: String) {
		verifyRoute(route)

		backStack.removeLastOrNull()
		backStack.add(route)
	}

	fun back() {
		backStack.removeLastOrNull()
	}
}

val LocalRouter = compositionLocalOf<Router> { error("No router provided") }

@Composable
fun ProvideRouter(
	routes: Map<String, @Composable () -> Unit>,
	defaultRoute: String,
	content: @Composable () -> Unit,
) {
	val backStack = remember { mutableStateListOf(defaultRoute) }
	val router = remember(routes, backStack) {
		Router(
			routes = routes,
			backStack = backStack,
		)
	}

	CompositionLocalProvider(
		LocalRouter provides router,
		content = content
	)
}

@Composable
fun RouterContent(
	router: Router = LocalRouter.current,
	fallbackRoute: String = "/"
) {
	NavDisplay(
		backStack = router.backStack,
		onBack = { router.back() },
		entryDecorators = listOf(
			rememberSaveableStateHolderNavEntryDecorator(),
		),
		entryProvider = { route ->
			NavEntry(route) {
				val route = router.resolve(route) ?: router.resolve(fallbackRoute)
				if (route != null) route()
			}
		}
	)
}
