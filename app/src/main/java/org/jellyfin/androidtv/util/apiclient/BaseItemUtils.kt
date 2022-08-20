@file:JvmName("BaseItemUtils")

package org.jellyfin.androidtv.util.apiclient

import android.content.Context
import android.text.format.DateFormat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.sdk.canPlay
import org.jellyfin.androidtv.util.sdk.compat.asSdk
import org.jellyfin.androidtv.util.sdk.getDisplayName
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.entities.LocationType
import org.jellyfin.apiclient.model.library.PlayAccess
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.serializer.toUUID
import org.koin.java.KoinJavaComponent.inject
import java.util.Calendar

// TODO Feature Envy!!! Wants to live in BaseItemDto.
fun BaseItemDto.isLiveTv() =
	baseItemType == BaseItemType.Program || baseItemType == BaseItemType.LiveTvChannel

fun BaseItemDto?.canPlay() = this?.asSdk()?.canPlay()

fun BaseItemDto.getFullName(context: Context): String? = when (baseItemType) {
	BaseItemType.Episode -> buildList {
		add(seriesName)

		if (parentIndexNumber == 0) {
			add(context.getString(R.string.episode_name_special))
		} else {
			if (parentIndexNumber != null)
				add(context.getString(R.string.lbl_season_number, parentIndexNumber))

			if (indexNumber != null && indexNumberEnd != null)
				add(context.getString(R.string.lbl_episode_range, indexNumber, indexNumberEnd))
			else if (indexNumber != null)
				add(context.getString(R.string.lbl_episode_number, indexNumber))

		}
	}.filterNot { it.isNullOrBlank() }.joinToString(" ")
	// we actually want the artist name if available
	BaseItemType.Audio,
	BaseItemType.MusicAlbum -> listOfNotNull(albumArtist, name)
		.filter { it.isNotEmpty() }
		.joinToString(" - ")
	else -> name
}

fun BaseItemDto.getDisplayName(context: Context): String = asSdk().getDisplayName(context)

fun BaseItemDto.getSubName(context: Context): String? = when (baseItemType) {
	BaseItemType.Episode -> when {
		locationType == LocationType.Virtual && name != null && premiereDate != null ->
			context.getString(
				R.string.lbl_name_date,
				name,
				TimeUtils.getFriendlyDate(context, TimeUtils.convertToLocalDate(premiereDate))
			)
		else -> name
	}
	BaseItemType.Season -> when {
		childCount != null && childCount > 0 -> when {
			childCount > 1 -> context.getString(R.string.lbl_num_episodes, childCount)
			else -> context.getString(R.string.lbl_one_episode)
		}
		else -> ""
	}
	BaseItemType.MusicAlbum -> when {
		childCount != null && childCount > 0 -> when {
			childCount > 1 -> context.getString(R.string.lbl_num_songs, childCount)
			else -> context.getString(R.string.lbl_one_song)
		}
		else -> ""
	}
	BaseItemType.Audio -> name
	else -> officialRating
}

fun org.jellyfin.sdk.model.api.BaseItemDto.getProgramUnknownChannelName(): String? =
	TvManager.getChannel(TvManager.getAllChannelsIndex(channelId?.toString())).name

fun org.jellyfin.sdk.model.api.BaseItemDto.getProgramSubText(context: Context) = buildString {
	// Add the channel name if set
	channelName?.let { append(channelName, " - ") }

	// Add the episode title if set
	episodeTitle?.let { append(episodeTitle, " ") }

	// If the start time is on a different day, add the date
	if (startDate?.dayOfYear != Calendar.getInstance()[Calendar.DAY_OF_YEAR])
		append(TimeUtils.getFriendlyDate(context, TimeUtils.getDate(startDate)), " ")

	// Add the start and end time
	val dateFormat = DateFormat.getTimeFormat(context)
	append(context.getString(
		R.string.lbl_time_range,
		dateFormat.format(startDate),
		dateFormat.format(endDate)
	))
}

fun BaseItemDto.getFirstPerson(searchedType: String) =
	people?.asSdk()?.firstOrNull { it.type == searchedType }

fun BaseItemDto.buildChapterItems(): List<ChapterItemInfo> {
	val apiClient by inject<ApiClient>(ApiClient::class.java)

	return chapters.mapIndexed { i, dto ->
		ChapterItemInfo().apply {
			itemId = id
			name = dto.name
			startPositionTicks = dto.startPositionTicks
			imagePath = when {
				dto.hasImage -> apiClient.imageApi.getItemImageUrl(
					itemId = itemId.toUUID(),
					imageType = ImageType.CHAPTER,
					tag = dto.imageTag,
					imageIndex = i,
				)
				else -> null
			}
		}
	}
}

fun org.jellyfin.sdk.model.api.BaseItemDto.isNew() = isSeries == true && isNews != true && isRepeat != true

fun SeriesTimerInfoDto.getSeriesOverview(context: Context) = buildString {
	if (recordNewOnly) appendLine(context.getString(R.string.lbl_record_only_new))
	else appendLine(context.getString(R.string.lbl_record_all))

	if (recordAnyChannel) appendLine(context.getString(R.string.lbl_on_any_channel))
	else appendLine(context.getString(R.string.lbl_on_channel, channelName))

	append(dayPattern)
	if (recordAnyTime) append(" ", context.getString(R.string.lbl_at_any_time))

	appendLine()
	when {
		prePaddingSeconds > 0 -> when {
			postPaddingSeconds > 0 -> append(
				context.getString(
					R.string.lbl_starting_early_ending_after,
					TimeUtils.formatSeconds(context, prePaddingSeconds),
					TimeUtils.formatSeconds(context, postPaddingSeconds)
				)
			)
			else -> append(
				context.getString(
					R.string.lbl_starting_early_ending_on_schedule,
					TimeUtils.formatSeconds(context, prePaddingSeconds)
				)
			)
		}
		else -> when {
			postPaddingSeconds > 0 -> append(
				context.getString(
					R.string.lbl_starting_on_schedule_ending_after,
					TimeUtils.formatSeconds(context, postPaddingSeconds)
				)
			)
			else -> append(context.getString(R.string.lbl_start_end_on_schedule))
		}
	}
}
