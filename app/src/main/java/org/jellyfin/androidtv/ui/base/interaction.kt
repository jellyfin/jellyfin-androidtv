package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent

typealias InteractionTracker = () -> Unit

private val DefaultInteractionTracker: InteractionTracker = {}

val LocalInteractionTracker = staticCompositionLocalOf<InteractionTracker> { DefaultInteractionTracker }

@Composable
fun ProvideLocalInteractionTracker(
	interactionTracker: InteractionTracker,
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(
		LocalInteractionTracker provides interactionTracker
	) {
		Box(
			modifier = Modifier.interactionTracker(interactionTracker),
		) {
			content()
		}
	}
}

@Composable
fun Modifier.interactionTracker(
	interactionTracker: InteractionTracker = LocalInteractionTracker.current,
): Modifier = onPreviewKeyEvent {
	interactionTracker()
	false
}
