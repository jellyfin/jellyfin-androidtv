package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse
import org.jellyfin.androidtv.util.sdk.getFullName
import org.jellyfin.androidtv.util.sdk.getSubName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.extensions.ticks
import org.koin.core.component.get
import timber.log.Timber
import java.text.SimpleDateFormat

open class BaseItemDtoBaseRowItem @JvmOverloads constructor(
	index: Int = 0,
	item: BaseItemDto,
	preferParentThumb: Boolean = false,
	staticHeight: Boolean = false,
	selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
	preferSeriesPoster: Boolean = false,
) : BaseRowItem(
	baseRowType = when (item.type) {
		BaseItemKind.TV_CHANNEL -> BaseRowType.LiveTvChannel
		BaseItemKind.PROGRAM -> BaseRowType.LiveTvProgram
		BaseItemKind.RECORDING -> BaseRowType.LiveTvRecording
		else -> BaseRowType.BaseItem
	},
	index = index,
	staticHeight = staticHeight,
	preferParentThumb = preferParentThumb,
	selectAction = selectAction,
	baseItem = item,
	preferSeriesPoster = preferSeriesPoster,
) {
	override val showCardInfoOverlay
		get() = when (baseItem?.type) {
			BaseItemKind.FOLDER,
			BaseItemKind.PHOTO_ALBUM,
			BaseItemKind.USER_VIEW,
			BaseItemKind.COLLECTION_FOLDER,
			BaseItemKind.PHOTO,
			BaseItemKind.VIDEO,
			BaseItemKind.PERSON,
			BaseItemKind.PLAYLIST,
			BaseItemKind.MUSIC_ARTIST -> true

			else -> false
		}

	override fun getItemId() = baseItem?.id

	override fun getBaseItemType() = baseItem?.type
	override fun isFavorite() = baseItem?.userData?.isFavorite == true
	override fun isPlayed() = baseItem?.userData?.played == true

	override fun getChildCountStr(): String? {
		// Playlist
		if (baseItem?.type == BaseItemKind.PLAYLIST) {
			val childCount = baseItem?.cumulativeRunTimeTicks?.ticks?.let { duration ->
				TimeUtils.formatMillis(duration.inWholeMilliseconds)
			}
			if (childCount != null) return childCount
		}

		// Folder
		if (baseItem?.isFolder == true && baseItem?.type != BaseItemKind.MUSIC_ARTIST) {
			val childCount = baseItem?.childCount
			if (childCount != null && childCount > 0) return childCount.toString()
		}

		// Default
		return null
	}

	override fun getCardName(context: Context) = when {
		baseItem?.type == BaseItemKind.AUDIO && baseItem?.albumArtist != null -> baseItem?.albumArtist
		baseItem?.type == BaseItemKind.AUDIO && baseItem?.album != null -> baseItem?.album
		else -> baseItem?.getFullName(context)
	}

	override fun getFullName(context: Context) = baseItem?.getFullName(context)
	override fun getName(context: Context) = when (baseItem?.type) {
		BaseItemKind.AUDIO -> baseItem?.getFullName(context)
		else -> baseItem?.name
	}

	override fun getSummary(context: Context) = baseItem?.overview

	override fun getSubText(context: Context) = when (baseItem?.type) {
		BaseItemKind.TV_CHANNEL -> baseItem?.number
		BaseItemKind.TV_PROGRAM,
		BaseItemKind.PROGRAM -> baseItem?.episodeTitle ?: baseItem?.channelName
		BaseItemKind.RECORDING -> {
			val title = listOfNotNull(
				baseItem?.channelName,
				baseItem?.episodeTitle
			).joinToString(" - ")

			val timestamp = buildString {
				append(SimpleDateFormat("d MMM").format(TimeUtils.getDate(baseItem!!.startDate)))
				append(" ")
				append(
					(DateFormat.getTimeFormat(context)
						.format(TimeUtils.getDate(baseItem!!.startDate)))
				)
				append(" - ")
				append(
					DateFormat.getTimeFormat(context).format(TimeUtils.getDate(baseItem!!.endDate))
				)
			}

			"$title $timestamp"
		}

		else -> baseItem?.getSubName(context)
	}

	override fun getImageUrl(
		context: Context,
		imageType: ImageType,
		fillWidth: Int,
		fillHeight: Int
	): String? {
		val seriesId = baseItem?.seriesId
		val seriesPrimaryImageTag = baseItem?.seriesPrimaryImageTag

		return when {
			preferSeriesPoster && seriesId != null && seriesPrimaryImageTag != null -> {
				imageHelper.getImageUrl(
					seriesId,
					org.jellyfin.sdk.model.api.ImageType.PRIMARY,
					seriesPrimaryImageTag
				)
			}

			imageType == ImageType.BANNER -> imageHelper.getBannerImageUrl(
				requireNotNull(
					baseItem
				), fillWidth, fillHeight
			)

			imageType == ImageType.THUMB -> imageHelper.getThumbImageUrl(
				requireNotNull(
					baseItem
				), fillWidth, fillHeight
			)

			else -> getPrimaryImageUrl(context, fillHeight)
		}
	}

	override fun getPrimaryImageUrl(
		context: Context,
		fillHeight: Int,
	) = imageHelper.getPrimaryImageUrl(
		baseItem!!,
		preferParentThumb,
		null,
		fillHeight
	)

	override fun getBadgeImage(context: Context) = when (baseItem?.type) {
		BaseItemKind.LIVE_TV_PROGRAM,
		BaseItemKind.PROGRAM -> when {
			baseItem?.seriesTimerId != null -> R.drawable.ic_record_series_red
			baseItem?.timerId != null -> R.drawable.ic_record_red

			else -> null
		}

		else -> when {
			baseItem?.criticRating != null -> when {
				baseItem!!.criticRating!! > 59f -> R.drawable.ic_rt_fresh
				else -> R.drawable.ic_rt_rotten
			}

			else -> null
		}
	}?.let { ContextCompat.getDrawable(context, it) }

	override fun refresh(
		outerResponse: LifecycleAwareResponse<BaseItemDto?>,
		scope: CoroutineScope,
	) {
		val itemId = baseItem?.id
		val api = get<ApiClient>()

		if (itemId == null) {
			Timber.w("Skipping call to BaseRowItem.refresh()")
			return
		}

		scope.launch(Dispatchers.IO) {
			baseItem = try {
				api.userLibraryApi.getItem(itemId = itemId).content
			} catch (err: ApiClientException) {
				Timber.e(err, "Failed to refresh item")
				null
			}

			if (outerResponse.active) withContext(Dispatchers.Main) {
				outerResponse.onResponse(baseItem)
			}
		}
	}
}
