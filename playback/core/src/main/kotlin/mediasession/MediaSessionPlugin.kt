package org.jellyfin.playback.core.mediasession

import android.content.Context
import org.jellyfin.playback.core.plugin.playbackPlugin

fun mediaSessionPlugin(
	androidContext: Context,
	options: MediaSessionOptions,
) = playbackPlugin {
	provide(MediaSessionService(androidContext, options))
}
