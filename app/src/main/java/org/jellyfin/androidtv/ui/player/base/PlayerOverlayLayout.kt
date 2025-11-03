package org.jellyfin.androidtv.ui.player.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerOverlayLayout(
	modifier: Modifier = Modifier,
	visibilityState: PlayerOverlayVisibilityState = rememberPlayerOverlayVisibility(),
	header: (@Composable () -> Unit)? = null,
	controls: (@Composable () -> Unit)? = null,
) = Box(
	modifier = modifier
		.fillMaxSize()
		.focusable()
		.onPreviewKeyEvent {
			if (visibilityState.visible) visibilityState.show()
			false
		}
		.onKeyEvent {
			if (it.key == Key.Back && visibilityState.visible) {
				visibilityState.hide()
				true
			} else if (!it.nativeKeyEvent.isSystem && !visibilityState.visible) {
				visibilityState.show()
				true
			} else {
				false
			}
		}
) {
	if (header != null) {
		AnimatedVisibility(
			visible = visibilityState.visible,
			modifier = Modifier
				.align(Alignment.TopCenter),
			enter = slideInVertically() + fadeIn(),
			exit = slideOutVertically() + fadeOut(),
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight(1f / 3)
					.background(
						brush = Brush.verticalGradient(
							colors = listOf(
								Color.Black.copy(alpha = 0.8f),
								Color.Transparent,
							)
						)
					)
					.overscan()
			) {
				header()
			}
		}
	}

	if (controls != null) {
		AnimatedVisibility(
			visible = visibilityState.visible,
			modifier = Modifier
				.align(Alignment.BottomCenter),
			enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
			exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
		) {
			Box(
				contentAlignment = Alignment.BottomCenter,
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight(1f / 3)
					.background(
						brush = Brush.verticalGradient(
							colors = listOf(
								Color.Transparent,
								Color.Black.copy(alpha = 0.8f),
							)
						)
					)
					.overscan(),
			) {
				JellyfinTheme(
					colorScheme = JellyfinTheme.colorScheme.copy(
						button = Color.Transparent
					)
				) {
					controls()
				}
			}
		}
	}
}

data class PlayerOverlayVisibilityState(
	val visible: Boolean,

	val toggle: () -> Unit,
	val show: () -> Unit,
	val hide: () -> Unit
)

@Composable
fun rememberPlayerOverlayVisibility(
	timeout: Duration = 5.seconds
): PlayerOverlayVisibilityState {
	val scope = rememberCoroutineScope()
	var timerVisible by remember { mutableStateOf(false) }
	var timerJob by remember { mutableStateOf<Job?>(null) }
	var visible = timerVisible

	fun show() {
		timerVisible = true
		timerJob?.cancel()
		timerJob = scope.launch {
			delay(timeout)
			timerVisible = false
		}
	}

	fun hide() {
		timerVisible = false
		timerJob?.cancel()
		timerJob = null
	}

	fun toggle() {
		if (timerVisible) hide()
		else show()
	}

	// Force visibility when not the active window, reset timer when it changes
	// to make sure popups keep the overlay visible
	val windowInfo = LocalWindowInfo.current
	visible = visible || !windowInfo.isWindowFocused

	var previousIsWindowFocused by remember { mutableStateOf(windowInfo.isWindowFocused) }
	LaunchedEffect(windowInfo.isWindowFocused) {
		if (windowInfo.isWindowFocused != previousIsWindowFocused) show()
		previousIsWindowFocused = windowInfo.isWindowFocused
	}

	return PlayerOverlayVisibilityState(
		visible = visible,
		toggle = ::toggle,
		show = ::show,
		hide = ::hide
	)
}
