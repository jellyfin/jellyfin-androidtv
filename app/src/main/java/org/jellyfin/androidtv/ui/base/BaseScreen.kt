package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
@Stable
fun BaseScreen(
	content: @Composable () -> Unit,
) {
	// Work around a focus issue with our current fragment based navigation approuch
	// by always focussing the screen contents whenever the screen is created
	val focusRequester = remember { FocusRequester() }
	Box(
		modifier = Modifier
			.focusRequester(focusRequester)
			.focusGroup()
	) {
		content()
	}
	LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
}
