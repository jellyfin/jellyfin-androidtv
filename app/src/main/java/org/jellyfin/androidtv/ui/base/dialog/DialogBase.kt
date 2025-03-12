package org.jellyfin.androidtv.ui.base.dialog

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jellyfin.androidtv.ui.base.JellyfinTheme

@Composable
fun DialogBase(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.() -> Unit,
) {
	val popupPositionProvider = remember { DialogPositionProvider() }

	val transition = updateTransition(visible)
	val alpha by transition.animateFloat(
		targetValueByState = { visible -> if (visible) 1f else 0f }
	)

	if (alpha != 0f) {
		val focusRequester = remember { FocusRequester() }

		Popup(
			onDismissRequest = onDismissRequest,
			properties = PopupProperties(
				focusable = true,
				dismissOnBackPress = true,
				dismissOnClickOutside = true,
			),
			popupPositionProvider = popupPositionProvider,
		) {
			val scrimColor = JellyfinTheme.colorScheme.scrim
			Box(
				modifier = modifier
					.fillMaxSize()
					.background(scrimColor.copy(alpha = scrimColor.alpha * alpha))
					.focusRequester(focusRequester)
					.focusGroup(),
				contentAlignment = Alignment.Center,
			) {
				content()
			}
		}

		LaunchedEffect(focusRequester) {
			focusRequester.requestFocus()
		}
	}
}
