package org.jellyfin.playback.core

import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.plugin.PlaybackPlugin
import org.jellyfin.playback.core.plugin.PlayerService

class PlaybackManagerBuilder() {
	private val factories = mutableListOf<PlaybackPlugin>()

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
		return PlaybackManager(backends.first(), services, mediaStreamResolvers)
	}
}

fun playbackManager(init: PlaybackManagerBuilder.() -> Unit): PlaybackManager =
	PlaybackManagerBuilder().apply { init() }.build()
