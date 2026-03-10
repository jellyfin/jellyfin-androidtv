package org.jellyfin.androidtv.ui.playback.overlay.action

import android.app.AlertDialog
import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.syncplay.SyncPlayRepository
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.androidtv.util.Utils
import org.koin.java.KoinJavaComponent

class SyncPlayAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	init {
		initializeWithIcon(R.drawable.ic_users)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		val repository: SyncPlayRepository = KoinJavaComponent.get(SyncPlayRepository::class.java)
		val videoQueueManager: VideoQueueManager = KoinJavaComponent.get(VideoQueueManager::class.java)

		// Keep group choices fresh so join flow mirrors other clients (List -> Join).
		repository.refreshGroups()
		val state = repository.state.value

		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)

		val actionLabels = mutableListOf<String>()
		val actionHandlers = mutableListOf<() -> Unit>()

		if (state.activeGroup == null) {
			actionLabels += context.getString(R.string.syncplay_create_group)
			actionHandlers += {
				repository.createGroup(context.getString(R.string.syncplay_default_group_name))
			}

			if (state.groups.isEmpty()) {
				actionLabels += context.getString(R.string.syncplay_refresh_groups)
				actionHandlers += { repository.refreshGroups() }
			} else {
				state.groups.forEach { group ->
					actionLabels += context.getString(R.string.syncplay_join_group_named, group.groupName)
					actionHandlers += { repository.joinGroup(group.groupId) }
				}
			}
		} else {
			actionLabels += context.getString(R.string.syncplay_sync_current)
			actionHandlers += {
				val items = videoQueueManager.getCurrentVideoQueue()
				val index = videoQueueManager.getCurrentMediaPosition()
				val itemIds = items.mapNotNull { it.id }
				if (itemIds.isEmpty() || index !in itemIds.indices) {
					Utils.showToast(context, R.string.syncplay_no_queue)
				} else {
					val positionTicks = playbackController.currentPosition * 10000
					repository.syncCurrentPlayback(itemIds, index, positionTicks, playbackController.isPlaying)
				}
			}

			actionLabels += context.getString(R.string.syncplay_leave_group)
			actionHandlers += { repository.leaveGroup() }
		}

		AlertDialog.Builder(context)
			.setTitle(context.getString(R.string.syncplay_title))
			.setItems(actionLabels.toTypedArray()) { _, which ->
				actionHandlers[which].invoke()
			}
			.setOnDismissListener { videoPlayerAdapter.leanbackOverlayFragment.setFading(true) }
			.show()
	}
}
