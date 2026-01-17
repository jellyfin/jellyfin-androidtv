package org.jellyfin.androidtv.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent

typealias RouteParameters = Map<String, String>
typealias RouteComposable = @Composable ((context: RouteContext) -> Unit)

data class RouteContext(
	val route: String,
	val parameters: RouteParameters,
)

class Router(
	val routes: Map<String, RouteComposable>,
	val backStack: SnapshotStateList<RouteContext>,
) {
	// Route resolving

	fun resolve(route: String): RouteComposable? = routes[route]
	fun verifyRoute(route: String, parameters: RouteParameters = emptyMap()) = require(resolve(route) != null) { "Invalid route $route" }

	// Route manipulation

	fun push(route: String, parameters: RouteParameters = emptyMap()) {
		verifyRoute(route, parameters)

		val context = RouteContext(route, parameters)
		backStack.add(context)
	}

	fun replace(route: String, parameters: RouteParameters = emptyMap()) {
		verifyRoute(route, parameters)

		val context = RouteContext(route, parameters)
		backStack.removeLastOrNull()
		backStack.add(context)
	}

	fun back() {
		backStack.removeLastOrNull()
	}
}

val LocalRouter = compositionLocalOf<Router> { error("No router provided") }
val LocalRouterTransitionScope = compositionLocalOf<SharedTransitionScope> { error("No router transition scope provided") }

@Composable
fun ProvideRouter(
	routes: Map<String, RouteComposable>,
	defaultRoute: String,
	defaultRouteParameters: RouteParameters = emptyMap(),
	content: @Composable () -> Unit,
) {
	val backStack = remember { mutableStateListOf(RouteContext(defaultRoute, defaultRouteParameters)) }
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
	fallbackRoute: String = "/",
	transitionSpec: AnimatedContentTransitionScope<Scene<RouteContext>>.() -> ContentTransform = defaultTransitionSpec(),
	popTransitionSpec: AnimatedContentTransitionScope<Scene<RouteContext>>.() -> ContentTransform = defaultPopTransitionSpec(),
	predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<RouteContext>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = { popTransitionSpec() },
) {
	SharedTransitionLayout {
		CompositionLocalProvider(LocalRouterTransitionScope provides this@SharedTransitionLayout) {
			NavDisplay(
				backStack = router.backStack,
				onBack = { router.back() },
				entryDecorators = listOf(
					rememberSaveableStateHolderNavEntryDecorator(),
				),
				transitionSpec = transitionSpec,
				popTransitionSpec = popTransitionSpec,
				predictivePopTransitionSpec = predictivePopTransitionSpec,
				entryProvider = { backStackEntry ->
					NavEntry(backStackEntry) {
						val route = backStackEntry.route
						val composable = router.resolve(route)
						if (composable == null) {
							val fallbackComposable = router.resolve(fallbackRoute)
								?: error("Unknown route $route, fallback $fallbackRoute is invalid")
							val context = backStackEntry.copy(route = fallbackRoute)
							fallbackComposable(context)
						} else {
							composable(backStackEntry)
						}
					}
				}
			)
		}
	}
}
