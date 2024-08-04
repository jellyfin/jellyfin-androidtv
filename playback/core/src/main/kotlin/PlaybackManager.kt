package org.jellyfin.playback.core

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jellyfin.playback.core.backend.BackendService
import org.jellyfin.playback.core.backend.PlayerBackend
import org.jellyfin.playback.core.plugin.PlayerService
import timber.log.Timber
import kotlin.reflect.KClass

class PlaybackManager internal constructor(
	val backend: PlayerBackend,
	private val services: MutableList<PlayerService>,
	val options: PlaybackManagerOptions,
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
		services.forEach { it.initialize(this, state, Job(job)) }
	}

	fun addService(service: PlayerService) {
		Timber.i("Adding service $service")
		service.initialize(this, state, Job(job))
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
