package org.jellyfin.playback.core.queue

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.requiredElement
import kotlin.time.Duration

/** Initial state applied before a stream starts, including preparation without autoplay. */
data class InitialPlayback(val position: Duration = Duration.ZERO, val playWhenReady: Boolean = true)

private val initialPlaybackKey = ElementKey<InitialPlayback>("InitialPlayback")
var QueueEntry.initialPlayback by requiredElement(initialPlaybackKey) { InitialPlayback() }
