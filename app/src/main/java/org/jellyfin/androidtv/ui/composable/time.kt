package org.jellyfin.androidtv.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * A composable that returns the current time in milliseconds. Updates every second.
 */
@Composable
fun rememberCurrentTime(): Long {
	var value by remember { mutableLongStateOf(System.currentTimeMillis()) }

	LaunchedEffect(Unit) {
		while (true) {
			value = System.currentTimeMillis()
			delay(1.seconds)
		}
	}

	return value
}
