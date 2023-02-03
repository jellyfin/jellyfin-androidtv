package org.jellyfin.playback.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.seconds

class PlaybackManagerOptions {
	var defaultRewindAmount = MutableStateFlow(10.seconds)
	var defaultFastForwardAmount = MutableStateFlow(10.seconds)
}
