package org.jellyfin.androidtv.syncplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.sdk.model.UUID

class SyncPlayViewModel(
	private val repository: SyncPlayRepository,
	private val videoQueueManager: VideoQueueManager,
	private val playbackControllerContainer: PlaybackControllerContainer,
) : ViewModel() {
	val state = repository.state

	fun refreshGroups() = repository.refreshGroups()

	fun createGroup(name: String) = repository.createGroup(name)

	fun joinGroup(groupId: UUID) = repository.joinGroup(groupId)

	fun leaveGroup() = repository.leaveGroup()

	fun syncCurrentPlayback() {
		viewModelScope.launch {
			val items = videoQueueManager.getCurrentVideoQueue()
			val index = videoQueueManager.getCurrentMediaPosition()
			val controller = playbackControllerContainer.playbackController

			val itemIds = items.mapNotNull { it.id }
			val positionTicks = (controller?.currentPosition ?: 0L) * 10000
			val isPlaying = controller?.isPlaying ?: false

			repository.syncCurrentPlayback(itemIds, index, positionTicks, isPlaying)
		}
	}
}
