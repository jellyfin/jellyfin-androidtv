package org.jellyfin.androidtv.ui.itemdetail

import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.preference.constant.PreferredVideoPlayer
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse
import org.jellyfin.androidtv.util.apiclient.PlaybackHelper
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.sdk.compat.asSdk
import org.jellyfin.androidtv.util.showIfNotEmpty
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

fun FullDetailsFragment.deleteItem(
	api: ApiClient,
	item: BaseItemDto,
	dataRefreshService: DataRefreshService,
	navigationRepository: NavigationRepository,
) = lifecycleScope.launch {
	Timber.i("Deleting item ${item.name} (id=${item.id})")

	try {
		withContext(Dispatchers.IO) {
			api.libraryApi.deleteItem(item.id)
		}
	} catch (error: ApiClientException) {
		Timber.e(error, "Failed to delete item ${item.name} (id=${item.id})")
		Toast.makeText(context, getString(R.string.item_deletion_failed, item.name), Toast.LENGTH_LONG).show()
		return@launch
	}

	dataRefreshService.lastDeletedItemId = item.id

	if (navigationRepository.canGoBack) navigationRepository.goBack()
	else navigationRepository.navigate(Destinations.home)

	Toast.makeText(context, getString(R.string.item_deleted, item.name), Toast.LENGTH_LONG).show()
}

fun FullDetailsFragment.showDetailsMenu(
	view: View,
	baseItemDto: BaseItemDto,
) = popupMenu(requireContext(), view) {
	// for each button check if it exists (not-null) and is invisible (overflow prevention)
	if (queueButton?.isVisible == false) {
		item(getString(R.string.lbl_add_to_queue)) { addItemToQueue() }
	}

	if (shuffleButton?.isVisible == false) {
		item(getString(R.string.lbl_shuffle_all)) { shufflePlay() }
	}

	if (trailerButton?.isVisible == false) {
		item(getString(R.string.lbl_play_trailers)) { playTrailers() }
	}

	if (favButton?.isVisible == false) {
		val favoriteStringRes = when (baseItemDto.userData?.isFavorite) {
			true -> R.string.lbl_remove_favorite
			else -> R.string.lbl_add_favorite
		}

		item(getString(favoriteStringRes)) { toggleFavorite() }
	}

	if (goToSeriesButton?.isVisible == false) {
		item(getString(R.string.lbl_goto_series)) { gotoSeries() }
	}
}.showIfNotEmpty()

fun FullDetailsFragment.showPlayWithMenu(
	view: View,
	shuffle: Boolean,
) = popupMenu(requireContext(), view) {
	item(getString(R.string.play_with_exo_player)) {
		systemPreferences.value[SystemPreferences.chosenPlayer] = PreferredVideoPlayer.EXOPLAYER
		play(mBaseItem, 0, shuffle)
	}

	item(getString(R.string.play_with_vlc)) {
		systemPreferences.value[SystemPreferences.chosenPlayer] = PreferredVideoPlayer.VLC
		play(mBaseItem, 0, shuffle)
	}

	item(getString(R.string.play_with_external_app)) {
		systemPreferences.value[SystemPreferences.chosenPlayer] = PreferredVideoPlayer.EXTERNAL

		val baseItem = mBaseItem.asSdk()
		val itemsCallback = object : LifecycleAwareResponse<List<BaseItemDto>>(lifecycle) {
			override fun onResponse(response: List<BaseItemDto>) {
				if (!active) return

				if (baseItem.type == BaseItemKind.MUSIC_ARTIST) {
					mediaManager.value.playNow(requireContext(), response, false)
				} else {
					videoQueueManager.value.setCurrentVideoQueue(response)
					navigationRepository.value.navigate(Destinations.externalPlayer(0))
				}
			}
		}
		PlaybackHelper.getItemsToPlay(baseItem, false, shuffle, itemsCallback)
	}
}.show()
