package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.sdk.getDisplayName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.ImageType

class NextUpViewModel(
	private val context: Context,
	private val api: ApiClient,
	private val userPreferences: UserPreferences,
	private val imageHelper: ImageHelper,
) : ViewModel() {
	private val _item = MutableStateFlow<NextUpItemData?>(null)
	val item: StateFlow<NextUpItemData?> = _item
	private val _state = MutableStateFlow(NextUpState.INITIALIZED)
	val state: StateFlow<NextUpState> = _state

	fun setItemId(id: UUID?) = viewModelScope.launch {
		if (id == null) {
			_item.value = null
			_state.value = NextUpState.NO_DATA
		} else {
			_item.value = loadItemData(id)
		}
	}

	fun playNext() {
		_state.value = NextUpState.PLAY_NEXT
	}

	fun close() {
		_state.value = NextUpState.CLOSE
	}

	private suspend fun loadItemData(id: UUID) = withContext(Dispatchers.IO) {
		val item by api.userLibraryApi.getItem(itemId = id)

		val thumbnail = when (userPreferences[UserPreferences.nextUpBehavior]) {
			NextUpBehavior.EXTENDED -> imageHelper.getPrimaryImageUrl(item)
			else -> null
		}
		val thumbnailBlurhash = item.imageBlurHashes?.get(ImageType.PRIMARY)?.get(item.imageTags?.get(ImageType.PRIMARY))
		val logo = imageHelper.getLogoImageUrl(item)
		val title = item.getDisplayName(context)

		NextUpItemData(
			item,
			item.id,
			title,
			thumbnail?.let { NextUpItemData.Image(it, thumbnailBlurhash, item.primaryImageAspectRatio ?: 1.0) },
			logo?.let { NextUpItemData.Image(it, null, 1.0) }
		)
	}
}
