package org.jellyfin.androidtv.ui.base.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jellyfin.androidtv.ui.base.JellyfinTheme

@Composable
fun DialogBase(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	scrimColor: Color = JellyfinTheme.colorScheme.scrim,
	enterTransition: EnterTransition = fadeIn(tween(300)),
	exitTransition: ExitTransition = fadeOut(tween(300)),
	contentAlignment: Alignment = Alignment.Center,
	content: @Composable BoxScope.() -> Unit,
) {
	val transition = updateTransition(visible)
	val alpha by transition.animateFloat(
		transitionSpec = { tween(1000) },
		targetValueByState = { visible -> if (visible) 1f else 0f }
	)

	if (transition.currentState || transition.isRunning) {
		val focusRequester = remember { FocusRequester() }
		var contentVisible by remember { mutableStateOf(false) }

		Dialog(
			onDismissRequest = onDismissRequest,
			properties = DialogProperties(
				dismissOnBackPress = true,
				dismissOnClickOutside = true,
				usePlatformDefaultWidth = false,
				decorFitsSystemWindows = false
			),
		) {
			Box(
				modifier = modifier
					.fillMaxSize()
					.background(scrimColor.copy(alpha = scrimColor.alpha * alpha))
					.focusRequester(focusRequester)
					.focusGroup(),
				contentAlignment = contentAlignment,
			) {
				transition.AnimatedVisibility(
					visible = { it && contentVisible },
					enter = enterTransition,
					exit = exitTransition,
				) {
					content()
					LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
				}
			}
		}

		LaunchedEffect(contentVisible) { contentVisible = true }
	}
}
