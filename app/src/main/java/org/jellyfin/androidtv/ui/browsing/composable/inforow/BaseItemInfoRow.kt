package org.jellyfin.androidtv.ui.browsing.composable.inforow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.ui.composable.getResolutionName
import org.jellyfin.androidtv.util.sdk.getProgramSubText
import org.jellyfin.androidtv.util.sdk.getSeasonEpisodeName
import org.jellyfin.androidtv.util.sdk.isNew
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SeriesStatus
import org.jellyfin.sdk.model.api.VideoRangeType
import org.jellyfin.sdk.model.extensions.ticks
import org.koin.compose.koinInject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@Composable
fun InfoRowDate(
	item: BaseItemDto,
) {
	val date = when (item.type) {
		BaseItemKind.PERSON -> {
			val birthDate = item.premiereDate

			if (birthDate != null) {
				val age = ChronoUnit.YEARS.between(birthDate, item.endDate ?: LocalDateTime.now())
				val birthDateStr = birthDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
				stringResource(R.string.person_birthday_and_age, birthDateStr, age)
			} else null
		}

		BaseItemKind.PROGRAM, BaseItemKind.TV_CHANNEL -> when {
			item.startDate != null && item.endDate != null -> buildString {
				// Format: 20:00 - 21:30
				append(item.startDate?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
				append(" - ")
				append(item.endDate?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
			}

			else -> null
		}

		BaseItemKind.SERIES -> item.productionYear?.toString()

		else -> when {
			item.premiereDate != null -> item.premiereDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
			item.productionYear != null -> item.productionYear?.toString()
			else -> null
		}
	}

	if (date != null) {
		InfoRowItem(contentDescription = null) {
			Text(date)
		}
	}
}

@Composable
fun InfoRowSeriesStatus(
	item: BaseItemDto,
) {
	val seriesStatus = item.status?.let(SeriesStatus::fromNameOrNull)

	if (seriesStatus != null) {
		when (seriesStatus) {
			SeriesStatus.CONTINUING -> InfoRowItem(
				contentDescription = stringResource(R.string.lbl__continuing),
				colors = InfoRowColors.Green,
			) {
				Text(stringResource(R.string.lbl__continuing))
			}

			SeriesStatus.ENDED -> InfoRowItem(
				contentDescription = stringResource(R.string.lbl_ended),
				colors = InfoRowColors.Red,
			) {
				Text(stringResource(R.string.lbl_ended))
			}

			SeriesStatus.UNRELEASED -> InfoRowItem(
				contentDescription = stringResource(R.string.unreleased),
				colors = InfoRowColors.Default,
			) {
				Text(stringResource(R.string.unreleased))
			}
		}
	}
}

@Composable
fun BaseItemInfoRowRuntime(
	runTime: Duration,
	endTime: LocalDateTime?,
) {
	InfoRowItem(contentDescription = null) {
		Text(buildString {
			// Run time
			val runTimeMinutes = max(1, runTime.inWholeMinutes.toInt())
			append(pluralStringResource(R.plurals.minutes, runTimeMinutes, runTimeMinutes))

			// End time
			if (endTime != null) {
				// Format: (Ends 14:50)
				append(" (", stringResource(R.string.lbl_ends), " ")
				append(endTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
				append(")")
			}
		})
	}
}

@Composable
fun InfoRowSeasonEpisode(item: BaseItemDto) {
	val context = LocalContext.current
	val displayName = item.getSeasonEpisodeName(context)

	if (displayName.isNotBlank()) {
		InfoRowItem(contentDescription = null) {
			Text(displayName)
		}
	}
}

@Composable
fun InfoRowMediaDetails(item: BaseItemDto) {
	val videoStream = item.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }
	val audioStream = item.mediaStreams?.firstOrNull { it.type == MediaStreamType.AUDIO }

	// Subtitles
	if (item.hasSubtitles == true) {
		InfoRowItem(
			contentDescription = null,
			colors = InfoRowColors.Default,
		) {
			Text(stringResource(R.string.indicator_subtitles))
		}
	}

	// Video resolution
	if (videoStream?.width != null && videoStream.height != null) {
		val resolution = getResolutionName(
			width = videoStream.width ?: 0,
			height = videoStream.height ?: 0,
			interlaced = videoStream.isInterlaced,
		)

		InfoRowItem(
			contentDescription = null,
			colors = InfoRowColors.Default,
		) {
			Text(resolution)
		}
	}

	// Video stream
	val videoCodecName = when {
		!videoStream?.videoDoViTitle.isNullOrBlank() -> stringResource(R.string.dolby_vision)
		videoStream?.videoRangeType != null && videoStream.videoRangeType != VideoRangeType.SDR && videoStream.videoRangeType != VideoRangeType.UNKNOWN ->
			videoStream.videoRangeType.serialName.uppercase()

		else -> videoStream?.codec?.uppercase()
	}
	if (!videoCodecName.isNullOrBlank()) {
		InfoRowItem(
			contentDescription = null,
			colors = InfoRowColors.Default,
		) {
			Text(videoCodecName)
		}
	}

	// Audio stream
	val audioCodecName = when {
		audioStream?.profile?.contains("Dolby Atmos", ignoreCase = true) == true -> stringResource(R.string.dolby_atmos)
		audioStream?.profile?.contains("DTS:X", ignoreCase = true) == true -> stringResource(R.string.dts_x)
		audioStream?.profile?.contains("DTS:HD", ignoreCase = true) == true -> stringResource(R.string.dts_hd)
		else -> when (audioStream?.codec?.uppercase()) {
			"DCA" -> stringResource(R.string.dca)
			"AC3" -> stringResource(R.string.ac3)
			"EAC3" -> stringResource(R.string.eac3)
			else -> audioStream?.codec?.uppercase()
		}
	}
	if (!audioCodecName.isNullOrBlank()) {
		InfoRowItem(
			contentDescription = null,
			colors = InfoRowColors.Default,
		) {
			Text(audioCodecName)
		}
	}

	// Audio channel layout
	val audioChannelLayout = audioStream?.channelLayout?.uppercase()
	if (!audioChannelLayout.isNullOrBlank()) {
		InfoRowItem(
			contentDescription = null,
			colors = InfoRowColors.Default,
		) {
			Text(audioChannelLayout)
		}
	}
}

@Composable
fun BaseItemInfoRow(
	item: BaseItemDto,
	includeRuntime: Boolean,
) {
	val userPreferences = koinInject<UserPreferences>()
	val ratingType = userPreferences[UserPreferences.defaultRatingType]

	val endTime = when (userPreferences[UserPreferences.clockBehavior]) {
		ClockBehavior.ALWAYS,
		ClockBehavior.IN_MENUS -> {
			val runTime = item.runTimeTicks?.ticks
			val progress = item.userData?.playbackPositionTicks?.ticks ?: Duration.ZERO
			if (runTime != null) LocalDateTime.now() + (runTime - progress).toJavaDuration()
			else null
		}

		else -> null
	}

	Row(
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		if (ratingType != RatingType.RATING_HIDDEN) {
			item.communityRating?.let { InfoRowCommunityRating(it / 10f) }
			item.criticRating?.let { InfoRowCriticRating(it / 100f) }
		}

		when (item.type) {
			BaseItemKind.EPISODE -> {
				InfoRowSeasonEpisode(item)
				InfoRowDate(item)
				if (includeRuntime) item.runTimeTicks?.ticks?.let { BaseItemInfoRowRuntime(it, endTime) }
				item.officialRating?.let { InfoRowParentalRating(it) }
				InfoRowMediaDetails(item)
			}

			BaseItemKind.BOX_SET -> {
				val countText = when {
					item.movieCount != null -> pluralStringResource(R.plurals.items, item.movieCount!!, item.movieCount!!)
					item.seriesCount != null -> pluralStringResource(R.plurals.items, item.seriesCount!!, item.seriesCount!!)
					item.childCount != null -> pluralStringResource(R.plurals.items, item.childCount!!, item.childCount!!)
					else -> null
				}
				if (countText != null) {
					InfoRowItem(contentDescription = null) {
						Text(countText)
					}
				}
				val runtime = item.cumulativeRunTimeTicks ?: item.runTimeTicks
				if (includeRuntime) runtime?.ticks?.let { BaseItemInfoRowRuntime(it, endTime) }
				item.officialRating?.let { InfoRowParentalRating(it) }
				InfoRowMediaDetails(item)
			}

			BaseItemKind.SERIES -> {
				InfoRowDate(item)
				if (includeRuntime) item.runTimeTicks?.ticks?.let { BaseItemInfoRowRuntime(it, null) }
				InfoRowSeriesStatus(item)
				item.officialRating?.let { InfoRowParentalRating(it) }
				InfoRowMediaDetails(item)
			}

			BaseItemKind.PROGRAM -> {
				val context = LocalContext.current
				InfoRowItem(contentDescription = null) {
					Text(item.getProgramSubText(context))
				}

				if (item.isNew()) {
					InfoRowItem(
						contentDescription = null,
						colors = InfoRowColors.Green,
					) {
						Text(stringResource(R.string.lbl_new))
					}
				}

				if (item.isSeries == true && item.isNews != true) {
					InfoRowItem(
						contentDescription = null,
						colors = InfoRowColors.Default,
					) {
						Text(stringResource(R.string.lbl_repeat))
					}
				}

				if (item.isLive == true) {
					InfoRowItem(
						contentDescription = null,
						colors = InfoRowColors.Default,
					) {
						Text(stringResource(R.string.lbl_live))
					}
				}

				if (includeRuntime) item.runTimeTicks?.ticks?.let { BaseItemInfoRowRuntime(it, endTime) }
				item.officialRating?.let { InfoRowParentalRating(it) }
				InfoRowMediaDetails(item)
			}

			BaseItemKind.MUSIC_ARTIST -> {
				// TODO: Appears to always be null? Maybe an API issue?
				val albums = item.albumCount ?: item.childCount ?: 0
				if (albums > 0) {
					InfoRowItem(contentDescription = null) {
						Text(pluralStringResource(R.plurals.albums, albums, albums))
					}
				}
			}

			BaseItemKind.MUSIC_ALBUM -> {
				val artist = item.albumArtist ?: item.albumArtists?.joinToString(", ")
				if (artist != null) {
					InfoRowItem(contentDescription = null) {
						Text(artist)
					}
				}
				InfoRowDate(item)
				val songs = item.songCount ?: item.childCount ?: 0
				if (songs > 0) {
					InfoRowItem(contentDescription = null) {
						Text(pluralStringResource(R.plurals.songs, songs, songs))
					}
				}
			}

			BaseItemKind.PLAYLIST -> {
				val items = item.childCount ?: 0
				if (items > 0) {
					InfoRowItem(contentDescription = null) {
						Text(pluralStringResource(R.plurals.items, items, items))
					}
				}

				val runtime = item.cumulativeRunTimeTicks ?: item.runTimeTicks
				if (includeRuntime) runtime?.ticks?.let { BaseItemInfoRowRuntime(it, endTime) }
				item.officialRating?.let { InfoRowParentalRating(it) }
				InfoRowMediaDetails(item)
			}

			else -> {
				InfoRowDate(item)
				if (includeRuntime) item.runTimeTicks?.ticks?.let { BaseItemInfoRowRuntime(it, endTime) }
				item.officialRating?.let { InfoRowParentalRating(it) }
				InfoRowMediaDetails(item)
			}
		}
	}
}

/**
 * Exposes the [BaseItemInfoRow] composable as Android view.
 */
class BaseItemInfoRowView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {
	private val _item = MutableStateFlow<BaseItemDto?>(null)
	private val _includeRuntime = MutableStateFlow(false)

	var item: BaseItemDto?
		get() = _item.value
		set(value) {
			_item.value = value
		}

	var includeRuntime: Boolean
		get() = _includeRuntime.value
		set(value) {
			_includeRuntime.value = value
		}

	init {
		isFocusable = false
		descendantFocusability = FOCUS_BLOCK_DESCENDANTS
	}

	@Composable
	override fun Content() {
		val item by _item.collectAsState()
		val includeRuntime by _includeRuntime.collectAsState()

		item?.let { BaseItemInfoRow(it, includeRuntime) }
	}
}
