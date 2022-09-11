package org.jellyfin.androidtv.util.sdk

import android.content.Context
import android.text.format.DateFormat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.PlayAccess
import java.util.Calendar

fun BaseItemDto.getDisplayName(context: Context): String {
	val seasonNumber = when {
		type == BaseItemKind.EPISODE
			&& parentIndexNumber != null
			&& parentIndexNumber != 0 ->
			context.getString(R.string.lbl_season_number, parentIndexNumber)
		else -> null
	}
	val episodeNumber = when {
		type != BaseItemKind.EPISODE -> indexNumber?.toString()
		parentIndexNumber == 0 -> context.getString(R.string.lbl_special)
		else -> indexNumber?.let { start ->
			indexNumberEnd?.let { end -> context.getString(R.string.lbl_episode_range, start, end) }
				?: context.getString(R.string.lbl_episode_number, start)
		}
	}
	val seasonEpisodeNumbers = listOfNotNull(seasonNumber, episodeNumber).joinToString(":")

	val nameSeparator = when (type) {
		BaseItemKind.EPISODE -> " — "
		else -> ". "
	}

	return listOfNotNull(seasonEpisodeNumbers, name)
		.filter { it.isNotEmpty() }
		.joinToString(nameSeparator)
}


fun BaseItemDto?.canPlay() = this != null
	&& playAccess == PlayAccess.FULL
	&& isPlaceHolder != true
	&& (type != BaseItemKind.EPISODE || locationType != LocationType.VIRTUAL)
	&& type != BaseItemKind.PERSON
	&& (isFolder != true || childCount?.takeIf { it > 0 } != null)

fun BaseItemDto.isLiveTv() = type == BaseItemKind.PROGRAM || type == BaseItemKind.LIVE_TV_CHANNEL
fun BaseItemDto.isNew() = isSeries == true && isNews != true && isRepeat != true

fun BaseItemDto.getProgramUnknownChannelName(): String? = TvManager.getChannel(TvManager.getAllChannelsIndex(channelId?.toString())).name

fun BaseItemDto.getProgramSubText(context: Context) = buildString {
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

fun BaseItemDto.getFirstPerson(searchedType: String) = people?.firstOrNull { it.type == searchedType }

fun BaseItemDto.getFullName(context: Context): String? = when (type) {
	BaseItemKind.EPISODE -> buildList {
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
	BaseItemKind.AUDIO,
	BaseItemKind.MUSIC_ALBUM -> listOfNotNull(albumArtist, name)
		.filter { it.isNotEmpty() }
		.joinToString(" - ")
	else -> name
}

fun BaseItemDto.getSubName(context: Context): String? = when (type) {
	BaseItemKind.EPISODE -> when {
		locationType == LocationType.VIRTUAL && name != null && premiereDate != null ->
			context.getString(
				R.string.lbl_name_date,
				name,
				TimeUtils.getFriendlyDate(context, TimeUtils.getDate(premiereDate))
			)
		else -> name
	}
	BaseItemKind.SEASON -> when {
		childCount != null && childCount!! > 0 -> when {
			childCount!! > 1 -> context.getString(R.string.lbl_num_episodes, childCount)
			else -> context.getString(R.string.lbl_one_episode)
		}
		else -> ""
	}
	BaseItemKind.MUSIC_ALBUM -> when {
		childCount != null && childCount!! > 0 -> when {
			childCount!! > 1 -> context.getString(R.string.lbl_num_songs, childCount)
			else -> context.getString(R.string.lbl_one_song)
		}
		else -> ""
	}
	BaseItemKind.AUDIO -> name
	else -> officialRating
}

fun BaseItemDto.buildChapterItems(api: ApiClient): List<ChapterItemInfo> = chapters?.mapIndexed { i, dto ->
	ChapterItemInfo(
		itemId = id,
		name = dto.name,
		startPositionTicks = dto.startPositionTicks,
		imagePath = when {
			dto.imageTag != null -> api.imageApi.getItemImageUrl(
				itemId = id,
				imageType = ImageType.CHAPTER,
				tag = dto.imageTag,
				imageIndex = i,
			)
			else -> null
		},
	)
}.orEmpty()
