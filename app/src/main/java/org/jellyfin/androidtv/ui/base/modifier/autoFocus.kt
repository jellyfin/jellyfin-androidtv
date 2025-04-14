package org.jellyfin.androidtv.ui.base.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onPlaced

/**
 * Automatically focus this composable once visible. Normally this would be used for a **single** composable in a screen/popup.
 * Items that can disappear will regain focus when being made visible later.
 */
@Composable
fun Modifier.autoFocus(
	focusRequester: FocusRequester = remember { FocusRequester() }
): Modifier = focusRequester(focusRequester)
	.onPlaced { focusRequester.requestFocus() }
