package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse
import org.jellyfin.androidtv.util.apiclient.getSeriesOverview
import org.jellyfin.androidtv.util.sdk.compat.asSdk
import org.jellyfin.androidtv.util.sdk.getFullName
import org.jellyfin.androidtv.util.sdk.getSubName
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUID
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber
import java.text.SimpleDateFormat
import org.jellyfin.apiclient.interaction.ApiClient as LegacyApiClient

open class BaseRowItem protected constructor(
	val baseRowType: BaseRowType,
	var index: Int = 0,
	val staticHeight: Boolean = false,
	val preferParentThumb: Boolean = false,
	val selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
	var playing: Boolean = false,
	var baseItem: org.jellyfin.sdk.model.api.BaseItemDto? = null,
	val basePerson: BaseItemPerson? = null,
	val chapterInfo: ChapterItemInfo? = null,
	val seriesTimerInfo: SeriesTimerInfoDto? = null,
	val gridButton: GridButton? = null,
	var preferSeriesPoster: Boolean = false,
) : KoinComponent {
	// Start of constructor hell
	@JvmOverloads
	constructor(
		index: Int = 0,
		item: BaseItemDto,
		preferParentThumb: Boolean = false,
		staticHeight: Boolean = false,
		selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
		preferSeriesPoster: Boolean = false,
	) : this(
		baseRowType = when (item.asSdk().type) {
			BaseItemKind.PROGRAM -> BaseRowType.LiveTvProgram
			BaseItemKind.RECORDING -> BaseRowType.LiveTvRecording
			else -> BaseRowType.BaseItem
		},
		index = index,
		staticHeight = staticHeight,
		preferParentThumb = preferParentThumb,
		selectAction = selectAction,
		baseItem = item.asSdk(),
		preferSeriesPoster = preferSeriesPoster,
	)

	@JvmOverloads
	constructor(
		item: org.jellyfin.sdk.model.api.BaseItemDto,
		index: Int = 0,
		preferParentThumb: Boolean = false,
		staticHeight: Boolean = false,
	) : this(
		index = index,
		preferParentThumb = preferParentThumb,
		staticHeight = staticHeight,
		baseRowType = when (item.type) {
			BaseItemKind.PROGRAM -> BaseRowType.LiveTvProgram
			BaseItemKind.RECORDING -> BaseRowType.LiveTvRecording
			else -> BaseRowType.BaseItem
		},
		baseItem = item,
	)

	constructor(
		index: Int,
		item: ChannelInfoDto,
	) : this(
		index = index,
		baseRowType = BaseRowType.LiveTvChannel,
		baseItem = item.asSdk(),
	)

	constructor(
		item: BaseItemDto,
		staticHeight: Boolean,
	) : this(
		index = 0,
		item = item,
		preferParentThumb = false,
		staticHeight = staticHeight,
	)

	constructor(
		item: SeriesTimerInfoDto,
	) : this(
		baseRowType = BaseRowType.SeriesTimer,
		seriesTimerInfo = item,
	)

	constructor(
		item: BaseItemPerson,
	) : this(
		baseRowType = BaseRowType.Person,
		staticHeight = true,
		basePerson = item,
	)

	constructor(
		item: ChapterItemInfo,
	) : this(
		baseRowType = BaseRowType.Chapter,
		staticHeight = true,
		chapterInfo = item,
	)

	constructor(
		item: GridButton,
	) : this(
		baseRowType = BaseRowType.GridButton,
		staticHeight = true,
		gridButton = item,
	)
	// End of constructor hell

	fun showCardInfoOverlay() = baseRowType == BaseRowType.BaseItem && listOf(
		BaseItemKind.FOLDER,
		BaseItemKind.PHOTO_ALBUM,
		BaseItemKind.USER_VIEW,
		BaseItemKind.COLLECTION_FOLDER,
		BaseItemKind.PHOTO,
		BaseItemKind.VIDEO,
		BaseItemKind.PERSON,
		BaseItemKind.PLAYLIST,
		BaseItemKind.MUSIC_ARTIST
	).contains(baseItem?.type)

	fun getImageUrl(context: Context, imageType: ImageType, fillWidth: Int, fillHeight: Int) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvProgram,
		BaseRowType.LiveTvRecording -> {
			val apiClient = get<LegacyApiClient>()
			val seriesId = baseItem?.seriesId
			val seriesPrimaryImageTag = baseItem?.seriesPrimaryImageTag

			when {
				preferSeriesPoster && seriesId != null && seriesPrimaryImageTag != null -> {
					ImageUtils.getImageUrl(seriesId.toString(), org.jellyfin.apiclient.model.entities.ImageType.Primary, seriesPrimaryImageTag)
				}

				imageType == ImageType.BANNER -> ImageUtils.getBannerImageUrl(baseItem, apiClient, fillWidth, fillHeight)
				imageType == ImageType.THUMB -> ImageUtils.getThumbImageUrl(baseItem, apiClient, fillWidth, fillHeight)
				else -> getPrimaryImageUrl(context, fillHeight)
			}
		}

		else -> getPrimaryImageUrl(context, fillHeight)
	}

	fun getPrimaryImageUrl(context: Context, fillHeight: Int) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvProgram,
		BaseRowType.LiveTvRecording -> ImageUtils.getPrimaryImageUrl(baseItem!!, preferParentThumb, null, fillHeight)

		BaseRowType.Person -> ImageUtils.getPrimaryImageUrl(basePerson!!, fillHeight)
		BaseRowType.Chapter -> chapterInfo?.imagePath
		BaseRowType.LiveTvChannel -> ImageUtils.getPrimaryImageUrl(baseItem!!)
		BaseRowType.GridButton -> gridButton?.imageRes?.let { ImageUtils.getResourceUrl(context, it) }
		BaseRowType.SeriesTimer -> ImageUtils.getResourceUrl(context, R.drawable.tile_land_series_timer)
	}

	fun getBaseItemType() = baseItem?.type

	fun isFavorite(): Boolean = baseItem?.userData?.isFavorite == true
	fun isFolder(): Boolean = baseItem?.isFolder == true
	fun isPlayed(): Boolean = baseItem?.userData?.played == true

	fun getCardName(context: Context): String? = when {
		baseItem?.type == BaseItemKind.AUDIO && baseItem!!.albumArtist != null -> baseItem!!.albumArtist
		baseItem?.type == BaseItemKind.AUDIO && baseItem!!.album != null -> baseItem!!.album
		else -> getFullName(context)
	}

	fun getFullName(context: Context) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvProgram,
		BaseRowType.LiveTvRecording -> baseItem?.getFullName(context)

		BaseRowType.Person -> basePerson?.name
		BaseRowType.Chapter -> chapterInfo?.name
		BaseRowType.LiveTvChannel -> baseItem?.name
		BaseRowType.GridButton -> gridButton?.text
		BaseRowType.SeriesTimer -> seriesTimerInfo?.name
	}

	fun getName(context: Context) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvRecording,
		BaseRowType.LiveTvProgram -> when (baseItem?.type) {
			BaseItemKind.AUDIO -> getFullName(context)
			else -> baseItem?.name
		}

		BaseRowType.Person -> basePerson?.name
		BaseRowType.Chapter -> chapterInfo?.name
		BaseRowType.LiveTvChannel -> baseItem?.name
		BaseRowType.GridButton -> gridButton?.text
		BaseRowType.SeriesTimer -> seriesTimerInfo?.name
	}

	fun getItemId() = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvProgram,
		BaseRowType.LiveTvChannel,
		BaseRowType.LiveTvRecording -> baseItem?.id.toString()

		BaseRowType.Person -> basePerson?.id?.toString()
		BaseRowType.Chapter -> chapterInfo?.itemId?.toString()
		BaseRowType.GridButton -> null
		BaseRowType.SeriesTimer -> seriesTimerInfo?.id
	}

	fun getSubText(context: Context) = when (baseRowType) {
		BaseRowType.BaseItem -> baseItem?.getSubName(context)
		BaseRowType.Person -> basePerson?.role
		BaseRowType.Chapter -> chapterInfo?.startPositionTicks?.ticks?.inWholeMilliseconds?.let(TimeUtils::formatMillis)
		BaseRowType.LiveTvChannel -> baseItem?.number
		BaseRowType.LiveTvProgram -> baseItem?.episodeTitle ?: baseItem?.channelName
		BaseRowType.LiveTvRecording -> {
			val title = listOfNotNull(
				baseItem?.channelName,
				baseItem?.episodeTitle
			).joinToString(" - ")

			val timestamp = buildString {
				append(SimpleDateFormat("d MMM").format(TimeUtils.getDate(baseItem!!.startDate)))
				append(" ")
				append((DateFormat.getTimeFormat(context).format(TimeUtils.getDate(baseItem!!.startDate))))
				append(" - ")
				append(DateFormat.getTimeFormat(context).format(TimeUtils.getDate(baseItem!!.endDate)))
			}

			"$title $timestamp"
		}

		BaseRowType.SeriesTimer -> {
			val channelName = if (seriesTimerInfo?.recordAnyChannel == true) "All Channels"
			else seriesTimerInfo?.channelName

			listOfNotNull(channelName, seriesTimerInfo?.dayPattern).joinToString(" ")
		}

		BaseRowType.GridButton -> ""
	}

	fun getSummary(context: Context) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvRecording,
		BaseRowType.LiveTvProgram -> baseItem?.overview

		BaseRowType.SeriesTimer -> seriesTimerInfo?.asSdk()?.getSeriesOverview(context)
		else -> null
	}.orEmpty()

	fun getChildCountStr(): String? {
		// Playlist
		if (baseItem?.type == BaseItemKind.PLAYLIST) {
			val childCount = baseItem?.cumulativeRunTimeTicks?.ticks?.inWholeMilliseconds?.let {
				TimeUtils.formatMillis(it)
			}
			if (childCount != null) return childCount
		}

		// Folder
		if (isFolder() && baseItem?.type != BaseItemKind.MUSIC_ARTIST) {
			val childCount = baseItem?.childCount
			if (childCount != null && childCount > 0) return childCount.toString()
		}

		// Default
		return null
	}

	fun getBadgeImage(context: Context): Drawable? {
		return when (baseRowType) {
			BaseRowType.BaseItem -> when {
				baseItem?.type == BaseItemKind.MOVIE && baseItem!!.criticRating != null -> when {
					baseItem!!.criticRating!! > 59f -> R.drawable.ic_rt_fresh
					else -> R.drawable.ic_rt_rotten
				}

				baseItem?.type == BaseItemKind.PROGRAM && baseItem!!.timerId != null -> when {
					baseItem!!.seriesTimerId != null -> R.drawable.ic_record_series_red
					else -> R.drawable.ic_record_red
				}

				else -> R.drawable.blank10x10
			}

			BaseRowType.Person,
			BaseRowType.LiveTvProgram -> when {
				baseItem?.seriesTimerId != null -> R.drawable.ic_record_series_red
				baseItem?.timerId != null -> R.drawable.ic_record_red
				else -> R.drawable.blank10x10
			}

			else -> R.drawable.blank10x10
		}.let { ContextCompat.getDrawable(context, it) }
	}

	@JvmOverloads
	fun refresh(
		outerResponse: LifecycleAwareResponse<org.jellyfin.sdk.model.api.BaseItemDto?>,
		scope: CoroutineScope = ProcessLifecycleOwner.get().lifecycleScope,
	) {
		if (baseRowType == BaseRowType.BaseItem) {
			val id = getItemId()
			val api = get<ApiClient>()

			if (id.isNullOrBlank()) {
				Timber.w("Skipping call to BaseRowItem.refresh()")
				return
			}

			scope.launch(Dispatchers.IO) {
				baseItem = try {
					api.userLibraryApi.getItem(itemId = id.toUUID()).content
				} catch (err: ApiClientException) {
					Timber.e(err, "Failed to refresh item")
					// TODO Only set to null when returned status is 404 (requires Jellyfin 10.9>=)
					null
				}

				if (outerResponse.active) withContext(Dispatchers.Main) {
					outerResponse.onResponse(baseItem)
				}
			}
		}
	}

	override fun equals(other: Any?): Boolean {
		if (other is BaseRowItem) return other.getItemId() == getItemId()
		return super.equals(other)
	}
}
