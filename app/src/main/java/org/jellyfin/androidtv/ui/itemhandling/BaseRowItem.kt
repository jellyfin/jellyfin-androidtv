package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.getSeriesOverview
import org.jellyfin.androidtv.util.sdk.compat.asSdk
import org.jellyfin.androidtv.util.sdk.getFullName
import org.jellyfin.androidtv.util.sdk.getSubName
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.interaction.EmptyResponse
import org.jellyfin.apiclient.interaction.Response
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.livetv.ChannelInfoDto
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.SearchHint
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.text.SimpleDateFormat

open class BaseRowItem private constructor(
	val baseRowType: BaseRowType,
	var index: Int = 0,
	val staticHeight: Boolean = false,
	val preferParentThumb: Boolean = false,
	val selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
	var playing: Boolean = false,
	var baseItem: BaseItemDto? = null,
	val basePerson: BaseItemPerson? = null,
	val chapterInfo: ChapterItemInfo? = null,
	val searchHint: SearchHint? = null,
	val channelInfo: ChannelInfoDto? = null,
	val seriesTimerInfo: SeriesTimerInfoDto? = null,
	val gridButton: GridButton? = null,
) {
	// Start of constructor hell
	@JvmOverloads
	constructor(
		index: Int = 0,
		item: BaseItemDto,
		preferParentThumb: Boolean = false,
		staticHeight: Boolean = false,
		selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
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
		baseItem = item,
	)

	constructor(
		index: Int,
		item: ChannelInfoDto,
	) : this(
		index = index,
		baseRowType = BaseRowType.LiveTvChannel,
		channelInfo = item,
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
		item: SearchHint,
	) : this(
		baseRowType = BaseRowType.SearchHint,
		searchHint = item,
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
	).contains(baseItem?.asSdk()?.type)

	fun getImageUrl(context: Context, imageType: ImageType, maxHeight: Int) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvProgram,
		BaseRowType.LiveTvRecording -> {
			val apiClient by inject<ApiClient>(ApiClient::class.java)
			when (imageType) {
				ImageType.BANNER -> ImageUtils.getBannerImageUrl(baseItem, apiClient, maxHeight)
				ImageType.THUMB -> ImageUtils.getThumbImageUrl(baseItem, apiClient, maxHeight)
				else -> getPrimaryImageUrl(context, maxHeight)
			}
		}
		else -> getPrimaryImageUrl(context, maxHeight)
	}

	fun getPrimaryImageUrl(context: Context, maxHeight: Int) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvProgram,
		BaseRowType.LiveTvRecording -> ImageUtils.getPrimaryImageUrl(baseItem!!, preferParentThumb, maxHeight)
		BaseRowType.Person -> ImageUtils.getPrimaryImageUrl(basePerson!!, maxHeight)
		BaseRowType.Chapter -> chapterInfo?.imagePath
		BaseRowType.LiveTvChannel -> {
			val apiClient by inject<ApiClient>(ApiClient::class.java)
			ImageUtils.getPrimaryImageUrl(channelInfo, apiClient)
		}
		BaseRowType.GridButton -> ImageUtils.getResourceUrl(context, gridButton!!.imageRes)
		BaseRowType.SeriesTimer -> ImageUtils.getResourceUrl(context, R.drawable.tile_land_series_timer)
		BaseRowType.SearchHint -> when {
			!searchHint?.primaryImageTag.isNullOrBlank() -> ImageUtils.getImageUrl(searchHint!!.itemId.toString(), org.jellyfin.apiclient.model.entities.ImageType.Primary, searchHint.primaryImageTag!!)
			!searchHint?.thumbImageItemId.isNullOrBlank() -> ImageUtils.getImageUrl(searchHint!!.thumbImageItemId!!, org.jellyfin.apiclient.model.entities.ImageType.Thumb, searchHint.thumbImageTag!!)
			else -> null
		}
	}

	fun getBaseItemType() = baseItem?.asSdk()?.type

	fun isFavorite(): Boolean = baseItem?.userData?.isFavorite == true
	fun isFolder(): Boolean = baseItem?.isFolderItem == true
	fun isPlayed(): Boolean = baseItem?.userData?.played == true

	fun getCardName(context: Context): String? {
		val item = baseItem?.asSdk()
		return when {
			item?.type == BaseItemKind.AUDIO && item.albumArtist != null -> item.albumArtist
			item?.type == BaseItemKind.AUDIO && item.album != null -> item.album
			else -> getFullName(context)
		}
	}

	fun getFullName(context: Context) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvProgram,
		BaseRowType.LiveTvRecording -> baseItem?.asSdk()?.getFullName(context)
		BaseRowType.Person -> basePerson?.name
		BaseRowType.Chapter -> chapterInfo?.name
		BaseRowType.LiveTvChannel -> channelInfo?.name
		BaseRowType.GridButton -> gridButton?.text
		BaseRowType.SeriesTimer -> seriesTimerInfo?.name
		BaseRowType.SearchHint -> listOfNotNull(searchHint?.series, searchHint?.name).joinToString(" - ")
	}

	fun getName(context: Context) = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvRecording,
		BaseRowType.LiveTvProgram -> when (baseItem?.asSdk()?.type) {
			BaseItemKind.AUDIO -> getFullName(context)
			else -> baseItem?.name
		}
		BaseRowType.Person -> basePerson?.name
		BaseRowType.Chapter -> chapterInfo?.name
		BaseRowType.SearchHint -> searchHint?.name
		BaseRowType.LiveTvChannel -> channelInfo?.name
		BaseRowType.GridButton -> gridButton?.text
		BaseRowType.SeriesTimer -> seriesTimerInfo?.name
	}

	fun getItemId() = when (baseRowType) {
		BaseRowType.BaseItem,
		BaseRowType.LiveTvProgram,
		BaseRowType.LiveTvRecording -> baseItem?.id
		BaseRowType.Person -> basePerson?.id?.toString()
		BaseRowType.Chapter -> chapterInfo?.itemId?.toString()
		BaseRowType.LiveTvChannel -> channelInfo?.id
		BaseRowType.GridButton -> null
		BaseRowType.SearchHint -> searchHint?.itemId?.toString()
		BaseRowType.SeriesTimer -> seriesTimerInfo?.id
	}

	fun getSubText(context: Context) = when (baseRowType) {
		BaseRowType.BaseItem -> baseItem?.asSdk()?.getSubName(context)
		BaseRowType.Person -> basePerson?.role
		BaseRowType.Chapter -> chapterInfo?.startPositionTicks?.div(10000)?.let(TimeUtils::formatMillis)
		BaseRowType.LiveTvChannel -> channelInfo?.number
		BaseRowType.LiveTvProgram -> baseItem?.episodeTitle ?: baseItem?.channelName
		BaseRowType.LiveTvRecording -> {
			val title = listOfNotNull(
				baseItem?.channelName,
				baseItem?.episodeTitle
			).joinToString(" - ")

			val timestamp = buildString {
				append(SimpleDateFormat("d MMM").format(TimeUtils.convertToLocalDate(baseItem!!.startDate)))
				append(" ")
				append((DateFormat.getTimeFormat(context).format(TimeUtils.convertToLocalDate(baseItem!!.startDate))))
				append(" - ")
				append(DateFormat.getTimeFormat(context).format(TimeUtils.convertToLocalDate(baseItem!!.endDate)))
			}

			"$title $timestamp"
		}
		BaseRowType.SearchHint -> searchHint?.type
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
		BaseRowType.SeriesTimer -> seriesTimerInfo?.getSeriesOverview(context)
		else -> null
	}.orEmpty()

	fun getRuntimeTicks() = when (baseRowType) {
		BaseRowType.LiveTvRecording,
		BaseRowType.BaseItem -> baseItem?.runTimeTicks ?: 0
		BaseRowType.LiveTvProgram -> {
			val start = baseItem?.startDate
			val end = baseItem?.endDate

			if (start != null && end != null) (end.time - start.time) * 10000
			else 0
		}
		else -> 0
	}

	fun getChildCountStr(): String? {
		// Playlist
		if (baseItem?.asSdk()?.type == BaseItemKind.PLAYLIST) {
			val childCount = baseItem?.cumulativeRunTimeTicks?.let {
				TimeUtils.formatMillis(it / 10000)
			}
			if (childCount != null) return childCount
		}

		// Folder
		if (isFolder() && baseItem?.asSdk()?.type != BaseItemKind.MUSIC_ARTIST) {
			val childCount = baseItem?.childCount
			if (childCount != null && childCount > 0) return childCount.toString()
		}

		// Default
		return null
	}

	fun getBadgeImage(context: Context): Drawable? {
		val item = baseItem?.asSdk()

		return when (baseRowType) {
			BaseRowType.BaseItem -> when {
				item?.type == BaseItemKind.MOVIE && item.criticRating != null -> when {
					item.criticRating!! > 59f -> R.drawable.ic_rt_fresh
					else -> R.drawable.ic_rt_rotten
				}
				item?.type == BaseItemKind.PROGRAM && item.timerId != null -> when {
					item.seriesTimerId != null -> R.drawable.ic_record_series_red
					else -> R.drawable.ic_record_red
				}
				else -> R.drawable.blank10x10
			}
			BaseRowType.Person,
			BaseRowType.LiveTvProgram -> when {
				item?.seriesTimerId != null -> R.drawable.ic_record_series_red
				item?.timerId != null -> R.drawable.ic_record_red
				else -> R.drawable.blank10x10
			}
			else -> R.drawable.blank10x10
		}.let { ContextCompat.getDrawable(context, it) }
	}

	// TODO rewrite with SDK (requires type change for [baseItem])
	fun refresh(outerResponse: EmptyResponse) {
		if (baseRowType == BaseRowType.BaseItem) {
			val id = getItemId()
			val apiClient by inject<ApiClient>(ApiClient::class.java)
			val user = get<UserRepository>(UserRepository::class.java).currentUser.value

			if (id.isNullOrBlank() || user == null) {
				Timber.w("Skipping call to BaseRowItem.refresh()")
				return
			}

			apiClient.GetItemAsync(id, user.id.toString(), object : Response<BaseItemDto>() {
				override fun onResponse(response: BaseItemDto) {
					baseItem = response
					outerResponse.onResponse()
				}
			})
		}
	}
}
