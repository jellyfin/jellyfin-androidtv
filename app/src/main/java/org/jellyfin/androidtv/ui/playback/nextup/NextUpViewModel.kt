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
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.util.apiclient.getItem
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.koin.java.KoinJavaComponent.inject

class NextUpViewModel(
	private val context: Context,
	private val apiClient: ApiClient
) : ViewModel() {
	private val _item = MutableLiveData<NextUpItemData?>()
	val item: LiveData<NextUpItemData?> = _item
	private val _state = MutableLiveData(NextUpState.INITIALIZED)
	val state: LiveData<NextUpState> = _state
	private val userPreferences by inject(UserPreferences::class.java)

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

	private fun BaseItemDto.getTitle(): String {
		val seasonNumber = when {
			this.baseItemType == BaseItemType.Episode
				&& this.parentIndexNumber != null
				&& this.parentIndexNumber != 0 ->
				context.getString(R.string.lbl_season_number, this.parentIndexNumber)
			else -> null
		}
		val episodeNumber = when {
			this.baseItemType != BaseItemType.Episode -> this.indexNumber?.toString()
			this.parentIndexNumber == 0 -> context.getString(R.string.lbl_special)
			this.indexNumber != null -> when {
				this.indexNumberEnd != null -> context.getString(R.string.lbl_episode_range, this.indexNumber, this.indexNumberEnd)
				else -> context.getString(R.string.lbl_episode_number, this.indexNumber)
			}
			else -> null
		}
		val seasonEpisodeNumbers = listOfNotNull(seasonNumber, episodeNumber).joinToString(":")

		val nameSeparator = when (this.baseItemType) {
			BaseItemType.Episode -> " — "
			else -> ". "
		}

		return listOfNotNull(seasonEpisodeNumbers, this.name)
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
