package org.jellyfin.androidtv.ui.player.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class VisibilityTimerState(
	val visible: Boolean,

	val toggle: () -> Unit,
	val show: () -> Unit,
	val hide: () -> Unit
)

@Composable
fun rememberVisibilityTimer(
	timeout: Duration = 5.seconds
): VisibilityTimerState {
	val scope = rememberCoroutineScope()
	var visible by remember { mutableStateOf(false) }
	var timerJob by remember { mutableStateOf<Job?>(null) }

	fun show() {
		visible = true
		timerJob?.cancel()
		timerJob = scope.launch {
			delay(timeout)
			visible = false
		}
	}

	fun hide() {
		visible = false
		timerJob?.cancel()
		timerJob = null
	}

	fun toggle() {
		if (visible) hide()
		else show()
	}

	return VisibilityTimerState(
		visible = visible,
		toggle = ::toggle,
		show = ::show,
		hide = ::hide
	)
}
