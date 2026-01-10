package org.jellyfin.androidtv.ui.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.model.PositionInfo
import org.jellyfin.playback.core.queue.queue
import org.koin.compose.koinInject
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberQueueEntry(
	playbackManager: PlaybackManager = koinInject(),
) = remember(playbackManager) {
	playbackManager.queue.entry
}.collectAsState()

@Composable
fun rememberPlayerPositionInfo(
	playbackManager: PlaybackManager = koinInject(),
	precision: Duration = 1.seconds
): MutableState<PositionInfo> {
	val playState by playbackManager.state.playState.collectAsState()
	val playing = playState == PlayState.PLAYING

	val positionInfo = remember { mutableStateOf(playbackManager.state.positionInfo) }

	LaunchedEffect(playing, precision) {
		val precisionMs = precision.inWholeMilliseconds

		while (playing) {
			positionInfo.value = playbackManager.state.positionInfo

			delay(precisionMs - (positionInfo.value.active.inWholeMilliseconds % precisionMs))
		}
	}

	return positionInfo
}

@Composable
fun rememberPlayerProgress(
	playbackManager: PlaybackManager = koinInject(),
): Float {
	val playState by playbackManager.state.playState.collectAsState()
	val active = playbackManager.state.positionInfo.active
	val duration = playbackManager.state.positionInfo.duration

	return rememberPlayerProgress(
		playing = playState == PlayState.PLAYING,
		active = active,
		duration = duration,
	)
}

@Composable
fun rememberPlayerProgress(
	playing: Boolean,
	active: Duration,
	duration: Duration,
): Float {
	val animatable = remember { Animatable(0f, 0f) }

	LaunchedEffect(playing, duration) {
		val activeMs = active.inWholeMilliseconds.toFloat()
		val durationMs = duration.inWholeMilliseconds.toFloat()

		if (active == Duration.ZERO) animatable.snapTo(0f)
		else animatable.snapTo((activeMs / durationMs).coerceIn(0f, 1f))

		if (playing) {
			animatable.animateTo(
				targetValue = 1f,
				animationSpec = tween(
					durationMillis = (durationMs - activeMs).roundToInt(),
					easing = LinearEasing,
				)
			)
		}
	}

	return animatable.value
}
