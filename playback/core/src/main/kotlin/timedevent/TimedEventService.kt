package org.jellyfin.playback.core.timedevent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.jellyfin.playback.core.plugin.PlayerService
import org.jellyfin.playback.core.queue.queue
import timber.log.Timber

class TimedEventService : PlayerService() {
	override suspend fun onInitialize() {
		manager.queue.entry
			.filterNotNull()
			.flatMapLatest { entry -> entry.timedEventsFlow }
			.onEach { timedEvents ->
				Timber.d("Timed events changed to $timedEvents")
				manager.backend.setTimedEvents(timedEvents.orEmpty())
			}.launchIn(coroutineScope + Dispatchers.Main)
	}
}
