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
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.dto.ImageOptions

class NextUpViewModel(
	private val context: Context,
	private val apiClient: ApiClient,
	private val userPreferences: UserPreferences
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

	private fun BaseItemDto.getTitle(): String {
		val seasonNumber = when {
			baseItemType == BaseItemType.Episode
				&& parentIndexNumber != null
				&& parentIndexNumber != 0 ->
				context.getString(R.string.lbl_season_number, parentIndexNumber)
			else -> null
		}
		val episodeNumber = when {
			baseItemType != BaseItemType.Episode -> indexNumber?.toString()
			parentIndexNumber == 0 -> context.getString(R.string.lbl_special)
			indexNumber != null -> when {
				indexNumberEnd != null -> context.getString(R.string.lbl_episode_range, indexNumber, indexNumberEnd)
				else -> context.getString(R.string.lbl_episode_number, indexNumber)
			}
			else -> null
		}
		val seasonEpisodeNumbers = listOfNotNull(seasonNumber, episodeNumber).joinToString(":")

		val nameSeparator = when (baseItemType) {
			BaseItemType.Episode -> " — "
			else -> ". "
		}

		return listOfNotNull(seasonEpisodeNumbers, name)
			.filter { it.isNotEmpty() }
			.joinToString(nameSeparator)
	}

	private suspend fun loadItemData(id: String) = withContext(Dispatchers.IO) {
		val item = apiClient.getItem(id) ?: return@withContext null

		val thumbnail = when (userPreferences[UserPreferences.nextUpBehavior]) {
			NextUpBehavior.EXTENDED -> apiClient.GetImageUrl(item, ImageOptions())
			else -> null
		}
		val logo = apiClient.GetLogoImageUrl(item, ImageOptions())
		val title = item.getTitle()

		NextUpItemData(
			item,
			item.id,
			title,
			thumbnail?.let { safelyLoadBitmap(it) },
			logo?.let { safelyLoadBitmap(it) }
		)
	}
}
