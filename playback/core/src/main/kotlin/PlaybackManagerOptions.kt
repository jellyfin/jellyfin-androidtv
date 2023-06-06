package org.jellyfin.playback.core

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

class PlaybackManagerOptions(
	val playerVolumeState: PlayerVolumeState,

	val defaultRewindAmount: StateFlow<Duration>,
	val defaultFastForwardAmount: StateFlow<Duration>,
)
