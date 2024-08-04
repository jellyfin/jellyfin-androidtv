package org.jellyfin.playback.core

import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.mediastream.MediaStreamResolver
import org.jellyfin.playback.core.mediastream.MediaStreamService
import org.jellyfin.playback.core.plugin.PlaybackPlugin
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.QueueService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PlaybackManagerBuilder(context: Context) {
	private val factories = mutableListOf<PlaybackPlugin>()
	private val volumeState = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) NoOpPlayerVolumeState()
	else AndroidPlayerVolumeState(audioManager = requireNotNull(context.getSystemService()))

	// Options
	var defaultRewindAmount: (() -> Duration)? = null
	var defaultFastForwardAmount: (() -> Duration)? = null

	fun install(pluginFactory: PlaybackPlugin) {
		factories.add(pluginFactory)
	}

	fun build(): PlaybackManager {
		val backends = mutableListOf<PlayerBackend>()
		val services = mutableListOf<PlayerService>()
		val mediaStreamResolvers = mutableListOf<MediaStreamResolver>()

		// Add plugins
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

		// Add default services
		services.add(QueueService())
		services.add(MediaStreamService(mediaStreamResolvers))

		// Only support a single backend right now
		require(backends.size == 1)
		val options = PlaybackManagerOptions(
			playerVolumeState = volumeState,
			defaultRewindAmount = defaultRewindAmount ?: { 10.seconds },
			defaultFastForwardAmount = defaultFastForwardAmount ?: { 10.seconds },
		)
		return PlaybackManager(backends.first(), services, options)
	}
}

fun playbackManager(context: Context, init: PlaybackManagerBuilder.() -> Unit): PlaybackManager =
	PlaybackManagerBuilder(context).apply { init() }.build()
