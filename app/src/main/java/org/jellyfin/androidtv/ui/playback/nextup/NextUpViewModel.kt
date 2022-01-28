package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.getDisplayName
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.androidtv.util.sdk.compat.asSdk
import org.jellyfin.apiclient.interaction.ApiClient

class NextUpViewModel(
	private val context: Context,
	private val apiClient: ApiClient,
	private val userPreferences: UserPreferences,
	private val sessionRepository: SessionRepository,
	private val imageHelper: ImageHelper,
) : ViewModel() {
	private val _item = MutableLiveData<NextUpItemData?>()
	val item: LiveData<NextUpItemData?> = _item
	private val _state = MutableLiveData(NextUpState.INITIALIZED)
	val state: LiveData<NextUpState> = _state

	fun setItemId(id: String?) = viewModelScope.launch {
		if (id == null) {
			_item.postValue(null)
			_state.postValue(NextUpState.NO_DATA)
		} else {
			val item = loadItemData(id)

			if (item == null) _state.postValue(NextUpState.NO_DATA)

			_item.postValue(item)
		}
	}

	fun playNext() {
		_state.postValue(NextUpState.PLAY_NEXT)
	}

	fun close() {
		_state.postValue(NextUpState.CLOSE)
	}

	private fun safelyLoadBitmap(url: String): Bitmap? = try {
		Glide.with(context)
			.asBitmap()
			.load(url)
			.submit()
			.get()
	} catch (e: Exception) {
		null
	}

	private suspend fun loadItemData(id: String) = withContext(Dispatchers.IO) {
		val userId = sessionRepository.currentSession.value?.userId ?: return@withContext null
		val item = apiClient.getItem(id, userId) ?: return@withContext null

		val thumbnail = when (userPreferences[UserPreferences.nextUpBehavior]) {
			NextUpBehavior.EXTENDED -> imageHelper.getPrimaryImageUrl(item.asSdk())
			else -> null
		}
		val logo = imageHelper.getLogoImageUrl(item.asSdk())
		val title = item.getDisplayName(context)

		NextUpItemData(
			item,
			item.id,
			title,
			thumbnail?.let { safelyLoadBitmap(it) },
			logo?.let { safelyLoadBitmap(it) }
		)
	}
}
