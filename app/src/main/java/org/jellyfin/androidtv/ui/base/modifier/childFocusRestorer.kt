package org.jellyfin.androidtv.ui.base.modifier

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester

/**
 * A focus group that automatically restores the focus to the child previously focused, instead of doing a focus search.
 * Any other focus related modifiers should be added after this one.
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.childFocusRestorer(
	focusRequester: FocusRequester = remember { FocusRequester() }
): Modifier = focusRequester(focusRequester)
	.focusProperties {
		exit = { focusRequester.saveFocusedChild(); FocusRequester.Default }
		enter = { if (focusRequester.restoreFocusedChild()) FocusRequester.Cancel else FocusRequester.Default }
	}
	.focusGroup()
