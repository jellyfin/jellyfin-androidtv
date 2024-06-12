package org.jellyfin.playback.exoplayer

import android.content.Context
import org.jellyfin.playback.core.plugin.playbackPlugin
import org.jellyfin.playback.exoplayer.session.MediaSessionOptions
import org.jellyfin.playback.exoplayer.session.MediaSessionService

fun exoPlayerPlugin(
	androidContext: Context,
	mediaSessionOptions: MediaSessionOptions,
) = playbackPlugin {
	provide(ExoPlayerBackend(androidContext))
	provide(MediaSessionService(androidContext, mediaSessionOptions))
}
