@file:JvmName("BaseItemUtils")

package org.jellyfin.androidtv.util.apiclient

import android.content.Context
import android.text.format.DateFormat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.apiclient.interaction.ApiClient
import org.jellyfin.apiclient.model.dto.BaseItemDto
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.jellyfin.apiclient.model.dto.ImageOptions
import org.jellyfin.apiclient.model.entities.ImageType
import org.jellyfin.apiclient.model.entities.LocationType
import org.jellyfin.apiclient.model.entities.PersonType
import org.jellyfin.apiclient.model.library.PlayAccess
import org.jellyfin.apiclient.model.livetv.SeriesTimerInfoDto
import org.koin.java.KoinJavaComponent.get
import java.util.*

// TODO Feature Envy!!! Wants to live in BaseItemDto.
fun BaseItemDto.isLiveTv() =
	baseItemType == BaseItemType.Program || baseItemType == BaseItemType.LiveTvChannel

fun BaseItemDto?.canPlay() = this != null
	&& playAccess == PlayAccess.Full
	&& ((isPlaceHolder == null || !isPlaceHolder)
	&& (baseItemType != BaseItemType.Episode || locationType != LocationType.Virtual))
	&& baseItemType != BaseItemType.Person
	&& baseItemType != BaseItemType.SeriesTimer
	&& (!isFolderItem || childCount == null || childCount > 0)

fun BaseItemDto.getFullName(context: Context): String? = when (baseItemType) {
	BaseItemType.Episode -> listOfNotNull(
		seriesName,
		parentIndexNumber?.let { context.getString(R.string.lbl_season_number, it) },
		indexNumber?.let { start ->
			indexNumberEnd?.let { end -> context.getString(R.string.lbl_episode_range, start, end) }
				?: context.getString(R.string.lbl_episode_number, start)
		}
	).filter { it.isNotEmpty() }.joinToString(" ")
	// we actually want the artist name if available
	BaseItemType.Audio,
	BaseItemType.MusicAlbum -> listOfNotNull(albumArtist, name)
		.filter { it.isNotEmpty() }
		.joinToString(" - ")
	else -> name
}

fun BaseItemDto.getDisplayName(context: Context): String {
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
		else -> indexNumber?.let { start ->
			indexNumberEnd?.let { end -> context.getString(R.string.lbl_episode_range, start, end) }
				?: context.getString(R.string.lbl_episode_number, start)
		}
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

fun BaseItemDto.getProgramUnknownChannelName(): String? =
	TvManager.getChannel(TvManager.getAllChannelsIndex(channelId)).name

fun BaseItemDto.getProgramSubText(context: Context) = buildString {
	// Add the channel name if set
	channelName?.let { append(channelName, " - ") }

	// Add the episode title if set
	episodeTitle?.let { append(episodeTitle, " ") }

	val startTime = Calendar.getInstance()
	startTime.time = TimeUtils.convertToLocalDate(startDate)

	// If the start time is on a different day, add the date
	if (startTime[Calendar.DAY_OF_YEAR] != Calendar.getInstance()[Calendar.DAY_OF_YEAR])
		append(TimeUtils.getFriendlyDate(context, startTime.time), " ")

	// Add the start and end time
	val dateFormat = DateFormat.getTimeFormat(context)
	append(context.getString(
		R.string.lbl_time_range,
		dateFormat.format(startTime.time),
		dateFormat.format(TimeUtils.convertToLocalDate(endDate))
	))
}

fun BaseItemDto.getFirstPerson(searchedType: PersonType) =
	people?.find { it.personType == searchedType }

fun BaseItemDto.buildChapterItems(): List<ChapterItemInfo> = chapters.mapIndexed { i, dto ->
	ChapterItemInfo().apply {
		itemId = id
		name = dto.name
		startPositionTicks = dto.startPositionTicks
		imagePath = when {
			dto.hasImage -> get<ApiClient>(ApiClient::class.java).GetImageUrl(id, ImageOptions().apply {
				imageType = ImageType.Chapter
				tag = dto.imageTag
				imageIndex = i
			})
			else -> null
		}
	}
}

fun BaseItemDto.isNew() = isSeries == true && isNews != true && isRepeat != true

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
