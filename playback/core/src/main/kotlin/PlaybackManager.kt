package org.jellyfin.playback.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import androidx.lifecycle.Lifecycle
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import androidx.lifecycle.Lifecycle
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import androidx.lifecycle.Lifecycle
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import androidx.lifecycle.Lifecycle
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.PlayerServiceType
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import androidx.lifecycle.Lifecycle
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import org.jellyfin.playback.core.plugin.ForegroundPlayerService
import timber.log.Timber
import kotlin.reflect.KClass

class PlaybackManager internal constructor(
	val backend: PlayerBackend,
	private val services: MutableList<PlayerService>,
	val options: PlaybackManagerOptions,
	private val lifecycle: Lifecycle? = null,
	private val lifecycle: Lifecycle? = null,
	private val lifecycle: Lifecycle? = null,
	private val lifecycle: Lifecycle? = null,
	private val lifecycle: Lifecycle? = null,
	parentJob: Job? = null,
) {
	internal val backendService = BackendService().also { service ->
		service.switchBackend(backend)
	}

	private val job = SupervisorJob(parentJob)
	val state: PlayerState = MutablePlayerState(
		options = options,
		backendService = backendService,
		queue = getService()
	)

	init {
		services.forEach { service ->
			val serviceType = determineServiceType(service)
			service.initialize(this, state, Job(job), lifecycle, serviceType)
		}
	}


	private fun determineServiceType(service: PlayerService): PlayerServiceType {
		return when {
			service is ForegroundPlayerService -> PlayerServiceType.FOREGROUND
			else -> PlayerServiceType.BACKGROUND
		}
	}
	fun addService(service: PlayerService) {

	private fun determineServiceType(service: PlayerService): PlayerServiceType {
		// Check if service has been configured with a specific type
		// This allows services to override the default through metadata
		return when {
			service is ForegroundPlayerService -> PlayerServiceType.FOREGROUND
			else -> PlayerServiceType.BACKGROUND
		}
	}

	private fun determineServiceType(service: PlayerService): PlayerServiceType {
		// Check if service implements ForegroundPlayerService interface
		return when {
			service is ForegroundPlayerService -> PlayerServiceType.FOREGROUND
			else -> PlayerServiceType.BACKGROUND
		}
	}
		Timber.i("Adding service $service")
		val serviceType = determineServiceType(service)
		service.initialize(this, state, Job(job), lifecycle, serviceType)

	private fun determineServiceType(service: PlayerService): PlayerServiceType {
		return when {
			service is ForegroundPlayerService -> PlayerServiceType.FOREGROUND
			else -> PlayerServiceType.BACKGROUND
		}
	}

		services.add(service)
	}

	fun <T : PlayerService> getService(kclass: KClass<T>): T? {
		for (service in services) {
			@Suppress("UNCHECKED_CAST")
			if (kclass.isInstance(service)) return service as T
		}
		return null
	}

	inline fun <reified T : PlayerService> getService() = getService(T::class)

	fun removeService(service: PlayerService) {
		Timber.i("Removing service $service")
		service.coroutineScope.cancel()
		services.remove(service)
	}
}
