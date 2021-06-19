@file:JvmName("BaseItemUtils")

package org.jellyfin.androidtv.util.apiclient

import android.content.Context
import android.text.format.DateFormat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.ui.livetv.TvManager
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.Utils
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

fun BaseItemDto.getFullName(context: Context) = when (baseItemType) {
	BaseItemType.Episode -> listOfNotNull(
		seriesName,
		when {
			parentIndexNumber != null -> context.getString(R.string.lbl_season_number, parentIndexNumber)
			else -> null
		},
		when {
			indexNumber != null -> when {
				indexNumberEnd != null -> context.getString(R.string.lbl_episode_range, indexNumber, indexNumberEnd)
				else -> context.getString(R.string.lbl_episode_number, indexNumber)
			}
			else -> null
		}
	).filter { it.isNotEmpty() }.joinToString(" ")
	// we actually want the artist name if available
	BaseItemType.Audio,
	BaseItemType.MusicAlbum -> listOfNotNull(albumArtist, name)
		.filter { it.isNotEmpty() }
		.joinToString(" - ")
	else -> name.orEmpty()
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

fun BaseItemDto.getSubName(context: Context) = when (baseItemType) {
	BaseItemType.Episode -> when {
		locationType == LocationType.Virtual && premiereDate != null ->
			"$name (" + TimeUtils.getFriendlyDate(context, TimeUtils.convertToLocalDate(premiereDate)) + ")"
		else -> name.orEmpty()
	}
	BaseItemType.Season -> when {
		childCount != null && childCount > 0 -> "$childCount " + context.getString(R.string.lbl_episodes)
		else -> ""
	}
	BaseItemType.MusicAlbum -> when {
		childCount != null && childCount > 0 ->
			"$childCount " + when {
				childCount > 1 -> context.getString(R.string.lbl_songs)
				else -> context.getString(R.string.lbl_song)
			}
		else -> ""
	}
	BaseItemType.Audio -> name.orEmpty()
	else -> officialRating.orEmpty()
}

fun BaseItemDto.getProgramUnknownChannelName() =
	TvManager.getChannel(TvManager.getAllChannelsIndex(channelId)).name.orEmpty()

fun BaseItemDto.getProgramSubText(context: Context) = buildString {
	// Add the channel name if set
	if (channelName != null) {
		append(channelName)
		append(" - ")
	}

	// Add the episode title if set
	if (episodeTitle != null) {
		append(episodeTitle)
		append(" ")
	}

	val startTime = Calendar.getInstance()
	startTime.time = TimeUtils.convertToLocalDate(startDate)

	// If the start time is on a different day, add the date
	if (startTime[Calendar.DAY_OF_YEAR] != Calendar.getInstance()[Calendar.DAY_OF_YEAR]) {
		append(TimeUtils.getFriendlyDate(context, startTime.time))
		append(" ")
	}

	// Add the start and end time
	val dateFormat = DateFormat.getTimeFormat(TvApp.getApplication())
	append(dateFormat.format(startTime.time))
	append("-")
	append(dateFormat.format(TimeUtils.convertToLocalDate(endDate)))
}

fun BaseItemDto.getFirstPerson(searchedType: PersonType) =
	people?.find { it.personType == searchedType }

fun BaseItemDto.buildChapterItems(): List<ChapterItemInfo> {
	val chapterItems = mutableListOf<ChapterItemInfo>()
	val options = ImageOptions().apply {
		imageType = ImageType.Chapter
	}
	chapters.mapIndexed { i, dto ->
		val chapter = ChapterItemInfo().apply {
			itemId = id
			name = dto.name
			startPositionTicks = dto.startPositionTicks
		}
		if (dto.hasImage) {
			options.tag = dto.imageTag
			options.imageIndex = i
			chapter.imagePath = get(ApiClient::class.java).GetImageUrl(id, options)
		}
		chapterItems.add(chapter)
	}
	return chapterItems
}

fun BaseItemDto.isNew() = Utils.isTrue(isSeries)
	&& !Utils.isTrue(isNews)
	&& !Utils.isTrue(isRepeat)

fun SeriesTimerInfoDto.getSeriesOverview(context: Context) = buildString {
	when{
		Utils.isTrue(recordNewOnly) -> append(context.getString(R.string.lbl_record_only_new))
		else -> append(context.getString(R.string.lbl_record_all))
	}

	append("\n")
	when {
		Utils.isTrue(recordAnyChannel) -> append(context.getString(R.string.lbl_on_any_channel))
		else -> append(context.getString(R.string.lbl_on_channel, channelName))
	}

	append("\n")
	append(dayPattern)
	if (Utils.isTrue(recordAnyTime)) {
		append(" ")
		append(context.getString(R.string.lbl_at_any_time))
	}

	append("\n")
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
