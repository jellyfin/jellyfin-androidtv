package org.jellyfin.androidtv.util

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.ui.itemdetail.firstUnplayedIndex
import org.jellyfin.androidtv.ui.playback.PlaybackLauncher
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.UUID

fun FragmentActivity.playFirstUnwatchedItem(parentId: UUID) {
	val api by inject<ApiClient>()
	val playbackHelper by inject<PlaybackHelper>()

	lifecycleScope.launch(Dispatchers.IO) {
		runCatching {
			api.itemsApi.getItems(
				parentId = parentId,
				recursive = true,
				isMissing = false,
				sortBy = setOf(ItemSortBy.SORT_NAME),
				sortOrder = setOf(SortOrder.ASCENDING),
				limit = 1,
				excludeItemTypes = setOf(),
				filters = setOf(ItemFilter.IS_UNPLAYED),
			)
		}.fold(
			onSuccess = { response ->
				val item = response.content.items?.firstOrNull()
				withContext(Dispatchers.Main) {
					if (item == null) {
						Toast.makeText(
							this@playFirstUnwatchedItem,
							R.string.msg_no_items,
							Toast.LENGTH_LONG
						).show()
					} else {
						playbackHelper.retrieveAndPlay(item.id, false, this@playFirstUnwatchedItem)
					}
				}
			},
			onFailure = { error ->
				Timber.e(error, "Error trying to play first unwatched")
				Toast.makeText(
					this@playFirstUnwatchedItem,
					R.string.msg_video_playback_error,
					Toast.LENGTH_LONG
				).show()
			},
		)
	}
}

fun FragmentActivity.resumePlaylist(playlistId: UUID) {
	val api by inject<ApiClient>()
	val playbackLauncher by inject<PlaybackLauncher>()
	val sessionRepository by inject<SessionRepository>()

	lifecycleScope.launch(Dispatchers.IO) {
		runCatching {
			api.playlistsApi.getPlaylistItems(
				playlistId = playlistId,
				userId = sessionRepository.currentSession.value?.userId,
				fields = setOf(
					ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
					ItemFields.MEDIA_SOURCE_COUNT,
					ItemFields.CHAPTERS,
					ItemFields.TRICKPLAY,
				),
				enableUserData = true,
			)
		}.fold(
			onSuccess = { response ->
				val items = response.content.items.orEmpty()
				val index = firstUnplayedIndex(items).coerceAtLeast(0)

				withContext(Dispatchers.Main) {
					if (items.isEmpty()) {
						Toast.makeText(
							this@resumePlaylist,
							R.string.msg_no_items,
							Toast.LENGTH_LONG
						).show()
					} else {
						playbackLauncher.launch(
							this@resumePlaylist,
							items,
							items[index].userData?.playbackPositionTicks?.let { (it / 10000).toInt() },
							false,
							index,
							false,
						)
					}
				}
			},
			onFailure = { error ->
				Timber.e(error, "Error trying to resume playlist")
				Toast.makeText(
					this@resumePlaylist,
					R.string.msg_video_playback_error,
					Toast.LENGTH_LONG
				).show()
			},
		)
	}
}
