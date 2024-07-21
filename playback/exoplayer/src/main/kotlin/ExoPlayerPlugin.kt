package org.jellyfin.playback.exoplayer

import android.content.Context
import org.jellyfin.playback.core.plugin.playbackPlugin

fun exoPlayerPlugin(
	androidContext: Context,
	exoPlayerOptions: ExoPlayerOptions = ExoPlayerOptions(),
) = playbackPlugin {
	provide(ExoPlayerBackend(androidContext, exoPlayerOptions))
}
