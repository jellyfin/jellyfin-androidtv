package org.jellyfin.playback.jellyfin.lyrics

import org.jellyfin.playback.core.element.ElementKey
import org.jellyfin.playback.core.element.element
import org.jellyfin.playback.core.element.elementFlow
import org.jellyfin.playback.core.queue.QueueEntry
import org.jellyfin.sdk.model.api.LyricDto

private val lyricsKey = ElementKey<LyricDto>("LyricDto")

/**
 * Get or set the [LyricDto] for this [QueueEntry].
 */
var QueueEntry.lyrics by element(lyricsKey)
val QueueEntry.lyricsFlow by elementFlow(lyricsKey)
