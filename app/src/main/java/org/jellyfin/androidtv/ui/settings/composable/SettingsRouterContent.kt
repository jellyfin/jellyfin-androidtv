package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import org.jellyfin.androidtv.ui.navigation.RouterContent

/**
 * Wrapper for the [RouterContent] composable that sets the settings slide in/out transitions.
 */
@Composable
fun SettingsRouterContent() {
	val duration = 400 // milliseconds

	val transitionIn = slideInHorizontally(
		initialOffsetX = { it },
		animationSpec = tween(duration, easing = FastOutSlowInEasing)
	) + fadeIn(
		animationSpec = tween(duration)
	)
	val transitionOut = slideOutHorizontally(
		targetOffsetX = { -it / 3 },
		animationSpec = tween(duration, easing = FastOutSlowInEasing)
	) + fadeOut(
		animationSpec = tween(duration)
	)

	val popIn = slideInHorizontally(
		initialOffsetX = { -it / 3 },
		animationSpec = tween(duration, easing = FastOutSlowInEasing)
	) + fadeIn(
		animationSpec = tween(duration)
	)

	val popOut = slideOutHorizontally(
		targetOffsetX = { it },
		animationSpec = tween(duration, easing = FastOutSlowInEasing)
	) + fadeOut(
		animationSpec = tween(duration)
	)

	RouterContent(
		transitionSpec = { transitionIn togetherWith transitionOut },
		popTransitionSpec = { popIn togetherWith popOut },
	)
}
