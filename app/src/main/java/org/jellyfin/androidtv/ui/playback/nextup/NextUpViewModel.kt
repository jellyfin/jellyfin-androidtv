package org.jellyfin.androidtv.ui.playback.nextup

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.koin.java.KoinJavaComponent

class NextUpViewModel(
	private val context: Context,
	private val apiClient: ApiClient
) : ViewModel() {
	private val _item = MutableLiveData<NextUpItemData?>()
	val item: LiveData<NextUpItemData?> = _item
	private val _state = MutableLiveData(NextUpState.INITIALIZED)
	val state: LiveData<NextUpState> = _state
	private val userPreferences by KoinJavaComponent.inject(UserPreferences::class.java)

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
		Glide.with(context).asBitmap().load(url).diskCacheStrategy(DiskCacheStrategy.RESOURCE).submit().get()
	} catch (e: Exception) {
		null
	}

	private suspend fun loadItemData(id: String) = withContext(Dispatchers.IO) {
		val nextUpFullEnabled = userPreferences[UserPreferences.nextUpFullEnabled]
		val item = apiClient.getItem(id) ?: return@withContext null

		val thumbnail = if (nextUpFullEnabled) apiClient.GetImageUrl(item, ImageOptions()) else null
		val logo = apiClient.GetLogoImageUrl(item, ImageOptions())

		val title = when {
			(item.indexNumber != null && item.indexNumberEnd != null && item.name != null && item.parentIndexNumber != 0) -> "${item.indexNumber}-${item.indexNumberEnd}. ${item.name}"
			(item.indexNumber != null && item.name != null && item.parentIndexNumber != 0) -> "${item.indexNumber}. ${item.name}"
			(item.name != null) -> item.name
			else -> ""
		}

		val overview = if (nextUpFullEnabled) item.overview else null

		NextUpItemData(
			item,
			item.id,
			title,
			overview,
			thumbnail?.let { safelyLoadBitmap(it) },
			logo?.let { safelyLoadBitmap(it) }
		)
	}
}
