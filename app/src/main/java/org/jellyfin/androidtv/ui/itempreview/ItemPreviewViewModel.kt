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
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.entities.SeriesStatus
import java.util.*

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
		val item = rowItem.baseItem

		val title = rowItem.loadTitle()
		val numbersString = rowItem.loadNumbersString()
		val subtitle = item.loadSubtitle()

		val logoImageUrl = if (item.hasLogo || item.parentLogoImageTag != null)
			ImageUtils.getLogoImageUrl(
				item,
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

	private fun BaseItemDto.loadSubtitle() : String = when {
		baseItemType == BaseItemType.Episode -> name
		baseItemType == BaseItemType.MusicAlbum -> name
		!taglines.isNullOrEmpty() -> taglines[0]
		!shortOverview.isNullOrEmpty() -> shortOverview
		!overview.isNullOrEmpty() -> overview
		else -> ""
	}

	private fun BaseRowItem.loadNumbersString() : SpannableStringBuilder {
		val numbersString = SpannableStringBuilder()

		if (baseItemType == BaseItemType.Episode) {
			if (baseItem.parentIndexNumber != null) {
				if (baseItem.parentIndexNumber == 0)
					numbersString.append(context.getString(R.string.lbl_special))
				else
					numbersString.append(
						context.getString(
							R.string.season_number_full,
							baseItem.parentIndexNumber
						)
					)
			}
			if (baseItem.indexNumber != null
				&& (baseItem.parentIndexNumber == null || baseItem.parentIndexNumber != 0)) {
				if (numbersString.isNotEmpty()) numbersString.append(" • ")

				if (baseItem.indexNumberEnd != null)
					numbersString.append(
						context.getString(
							R.string.episode_range_full,
							baseItem.indexNumber,
							baseItem.indexNumberEnd
						)
					)
				else
					numbersString.append(
						context.getString(
							R.string.episode_number_full,
							baseItem.indexNumber
						)
					)
			}
		} else {
			if (baseItem.productionYear != null) {
				if (baseItem.endDate != null) {
					val cal = Calendar.getInstance()
					cal.time = baseItem.endDate
					if (baseItem.productionYear != cal.get(Calendar.YEAR)) {
						numbersString.append(
							context.getString(
								R.string.num_range,
								baseItem.productionYear,
								cal.get(Calendar.YEAR)
							)
						)
					} else {
						numbersString.append(baseItem.productionYear.toString())
					}
				} else if (baseItemType == BaseItemType.Series
					&& baseItem.seriesStatus == SeriesStatus.Continuing) {
					numbersString.append(
						context.getString(
							R.string.year_to_present,
							baseItem.productionYear
						)
					)
				} else {
					numbersString.append(baseItem.productionYear.toString())
				}
			}
			if (!baseItem.officialRating.isNullOrEmpty()) {
				if (numbersString.isNotEmpty()) numbersString.append(" • ")
				numbersString.append(baseItem.officialRating)
			}
			if (baseItemType == BaseItemType.MusicAlbum && childCount > 0) {
				if (numbersString.isNotEmpty()) numbersString.append(" • ")
				numbersString.append(getSubText(context))
			}
		}

		if (baseItemType != BaseItemType.UserView && baseItemType != BaseItemType.CollectionFolder) {
			val ratingType = userPreferences[UserPreferences.defaultRatingType]
			if (ratingType == RatingType.RATING_TOMATOES && baseItem.criticRating != null) {
				val badge = if (baseItem.criticRating > 59)
					ContextCompat.getDrawable(context, R.drawable.ic_rt_fresh)
				else
					ContextCompat.getDrawable(context, R.drawable.ic_rt_rotten)
				if (badge != null) {
					badge.setBounds(
						0,
						0,
						35,
						35
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
						.append(Math.round(baseItem.criticRating).toString())
						.append("%")
				}
			} else if (ratingType == RatingType.RATING_STARS && baseItem.communityRating != null) {
				val badge = ContextCompat.getDrawable(context, R.drawable.ic_star)
				if (badge != null) {
					badge.setBounds(
						0,
						0,
						35,
						35
					)
					val imageSpan = ImageSpan(badge)
					if (numbersString.isNotEmpty()) numbersString.append("   ")
					numbersString.setSpan(
						imageSpan,
						numbersString.length - 1,
						numbersString.length,
						0
					)
					numbersString.append(" ").append(baseItem.communityRating.toString())
				}
			}
		}
		return numbersString
	}
}
