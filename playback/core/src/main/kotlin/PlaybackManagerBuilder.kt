package org.jellyfin.playback.core

import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.plugin.PlaybackPlugin
import org.jellyfin.playback.core.plugin.PlayerService

class PlaybackManagerBuilder(context: Context) {
	private val factories = mutableListOf<PlaybackPlugin>()
	private val volumeState = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) NoOpPlayerVolumeState()
	else AndroidPlayerVolumeState(audioManager = requireNotNull(context.getSystemService()))

	val options = PlaybackManagerOptions(volumeState)

	fun install(pluginFactory: PlaybackPlugin) {
		factories.add(pluginFactory)
	}

	fun build(): PlaybackManager {
		val backends = mutableListOf<PlayerBackend>()
		val services = mutableListOf<PlayerService>()
		val mediaStreamResolvers = mutableListOf<MediaStreamResolver>()

		val installContext = object : PlaybackPlugin.InstallContext {
			override fun provide(backend: PlayerBackend) {
				backends.add(backend)
			}

			override fun provide(service: PlayerService) {
				services.add(service)
			}

			override fun provide(mediaStreamResolver: MediaStreamResolver) {
				mediaStreamResolvers.add(mediaStreamResolver)
			}
		}

		for (factory in factories) factory.install(installContext)

		// Only support a single backend right now
		require(backends.size == 1)
		return PlaybackManager(backends.first(), services, mediaStreamResolvers, options)
	}
}

fun playbackManager(context: Context, init: PlaybackManagerBuilder.() -> Unit): PlaybackManager =
	PlaybackManagerBuilder(context).apply { init() }.build()
