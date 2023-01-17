package org.jellyfin.playback.exoplayer

import android.content.Context
import org.jellyfin.playback.core.plugin.playbackPlugin

fun exoPlayerPlugin(androidContext: Context) = playbackPlugin {
	provide(ExoPlayerBackend(androidContext))
}
