package org.jellyfin.androidtv.ui.itempreview

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.sdk.compat.asSdk
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import java.time.ZoneOffset
import java.util.*
import kotlin.math.roundToInt

class ItemPreviewViewModel(
	private val context: Context,
	private val apiClient: ApiClient,
	private val userPreferences: UserPreferences,
) : ViewModel() {
	private val _item = MutableLiveData<ItemPreviewItemData?>()
	val item: LiveData<ItemPreviewItemData?> = _item

	fun setItemData(rowItem: BaseRowItem?, rowHeader: String?) = viewModelScope.launch {
		if (rowItem == null || rowHeader == null) {
			_item.postValue(null)
		} else {
			val item = loadItemData(rowItem, rowHeader)
			_item.postValue(item)
		}
	}

	private suspend fun loadItemData(rowItem: BaseRowItem, rowHeader: String) = withContext(Dispatchers.IO) {
		val legacyItem = rowItem.baseItem
		val item = legacyItem.asSdk()

		val title = rowItem.loadTitle()
		val numbersString = item.loadNumbersString()
		val subtitle = item.loadSubtitle()

		val logoImageUrl =
			if (item.imageTags?.contains(ImageType.LOGO) == true || item.parentLogoImageTag != null) ImageUtils.getLogoImageUrl(
				legacyItem,
				apiClient,
				0,
				false
			) else null

		ItemPreviewItemData (
			item,
			title,
			numbersString,
			subtitle,
			logoImageUrl,
			rowHeader
		)
	}

	private fun BaseRowItem.loadTitle() : String = when (baseItemType) {
		BaseItemType.Episode -> baseItem.seriesName
		BaseItemType.MusicAlbum -> baseItem.albumArtist
		BaseItemType.CollectionFolder,
		BaseItemType.UserView -> ""
		else -> getCardName(context)
	}

	private fun BaseItemDto.loadSubtitle() : String? = when {
		type == "Episode" -> name
		type == "MusicAlbum" -> name
		!taglines.isNullOrEmpty() -> taglines!![0]
		!overview.isNullOrEmpty() -> overview
		else -> ""
	}

	private fun BaseItemDto.loadNumbersString() : SpannableStringBuilder {
		val numbersString = SpannableStringBuilder()

		if (type == "Episode") {
			if (parentIndexNumber != null) {
				if (parentIndexNumber == 0)
					numbersString.append(context.getString(R.string.lbl_special))
				else
					numbersString.append(
						context.getString(
							R.string.season_number_full,
							parentIndexNumber
						)
					)
			}
			if (indexNumber != null
				&& (parentIndexNumber == null || parentIndexNumber != 0)) {
				if (numbersString.isNotEmpty()) numbersString.append(" • ")

				if (indexNumberEnd != null)
					numbersString.append(
						context.getString(
							R.string.episode_range_full,
							indexNumber,
							indexNumberEnd
						)
					)
				else
					numbersString.append(
						context.getString(
							R.string.episode_number_full,
							indexNumber
						)
					)
			}
		} else {
			if (productionYear != null) {
				if (endDate != null) {
					val cal = Calendar.getInstance()
					cal.time = Date.from(endDate!!.toInstant(ZoneOffset.UTC))
					if (productionYear != cal.get(Calendar.YEAR)) {
						numbersString.append(
							context.getString(
								R.string.num_range,
								productionYear,
								cal.get(Calendar.YEAR)
							)
						)
					} else {
						numbersString.append(productionYear.toString())
					}
				} else if (type == "Series" && status == "Continuing") {
					numbersString.append(
						context.getString(
							R.string.year_to_present,
							productionYear
						)
					)
				} else {
					numbersString.append(productionYear.toString())
				}
			}
			if (!officialRating.isNullOrEmpty()) {
				if (numbersString.isNotEmpty()) numbersString.append(" • ")
				numbersString.append(officialRating)
			}
			if (type == "MusicAlbum" && childCount != null && childCount!! > 0) {
				if (numbersString.isNotEmpty()) numbersString.append(" • ")
				numbersString.append(
					when {
						childCount!! > 1 -> context.getString(R.string.lbl_num_songs, childCount)
						else -> context.getString(R.string.lbl_one_song)
					}
				)
			}
		}

		if (type != "UserView" && type != "CollectionFolder") {
			val ratingType = userPreferences[UserPreferences.defaultRatingType]
			if (ratingType == RatingType.RATING_TOMATOES && criticRating != null) {
				val badge = if (criticRating!! > 59)
					ContextCompat.getDrawable(context, R.drawable.ic_rt_fresh)
				else
					ContextCompat.getDrawable(context, R.drawable.ic_rt_rotten)
				if (badge != null) {
					badge.setBounds(
						0,
						0,
						33,
						33
					)
					val imageSpan = ImageSpan(badge)
					if (numbersString.isNotEmpty()) numbersString.append("   ")
					numbersString.setSpan(
						imageSpan,
						numbersString.length - 1,
						numbersString.length,
						0
					)
					numbersString.append(" ")
						.append((criticRating!!).roundToInt().toString())
						.append("%")
				}
			} else if (ratingType == RatingType.RATING_STARS && communityRating != null) {
				val badge = ContextCompat.getDrawable(context, R.drawable.ic_star)
				if (badge != null) {
					badge.setBounds(
						0,
						0,
						30,
						30
					)
					val imageSpan = ImageSpan(badge)
					if (numbersString.isNotEmpty()) numbersString.append("   ")
					numbersString.setSpan(
						imageSpan,
						numbersString.length - 1,
						numbersString.length,
						0
					)
					numbersString.append(" ").append(communityRating.toString())
				}
			}
		}
		return numbersString
	}
}
