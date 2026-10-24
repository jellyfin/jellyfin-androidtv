package org.jellyfin.androidtv.ui.composable.compat

import android.content.Context
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jellyfin.androidtv.ui.browsing.DestinationFragmentView
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationAction
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.koin.compose.koinInject

@Composable
fun AppNavigationHost(
	modifier: Modifier = Modifier,
	navigationRepository: NavigationRepository = koinInject(),
) {
	val factory = remember { AppNavigationHostViewFactory() }

	val currentDestination by navigationRepository.currentDestination.collectAsState()
	val canGoBack by remember {
		navigationRepository.currentAction.map { navigationRepository.canGoBack }.distinctUntilChanged()
	}.collectAsState(navigationRepository.canGoBack)

	// The back button is only disabled on the home screen, on all other screens it navigates back
	// to the previous screen or falls back to the home screen. Without this the app would be closed
	// when the back stack is empty, for example after the activity was restored from a saved state.
	val isHome = currentDestination?.fragment == Destinations.home.fragment

	BackHandler(canGoBack || !isHome) {
		// Fall back to the home screen if there is no previous screen to navigate back to
		if (!navigationRepository.goBack()) navigationRepository.reset(Destinations.home, clearHistory = true)
	}

	AndroidView(
		factory = factory,
		modifier = modifier,
	)

	LaunchedEffect(Unit) {
		navigationRepository.currentAction.collect { action ->
			when (action) {
				is NavigationAction.NavigateFragment -> factory.view.navigate(action)
				NavigationAction.GoBack -> factory.view.goBack()
				NavigationAction.Nothing -> Unit
			}
		}
	}
}

private class AppNavigationHostViewFactory : (Context) -> View {
	private var _view: DestinationFragmentView? = null

	val view get() = requireNotNull(_view)

	override operator fun invoke(
		context: Context
	): View = DestinationFragmentView(context).also { view ->
		_view = view
	}
}
