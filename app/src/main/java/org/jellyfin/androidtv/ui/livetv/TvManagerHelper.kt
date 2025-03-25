package org.jellyfin.androidtv.ui.livetv

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun BaseItemDto.copyWithLastPlayedDate(
	lastPlayedDate: LocalDateTime,
) = copy(
	userData = userData?.copy(
		lastPlayedDate = lastPlayedDate,
	)
)

fun loadLiveTvChannels(fragment: Fragment, callback: (channels: Collection<BaseItemDto>?) -> Unit) {
	val liveTvPreferences by fragment.inject<LiveTvPreferences>()
	val api by fragment.inject<ApiClient>()

	fragment.lifecycleScope.launch {
		val sortDatePlayed =
			liveTvPreferences[LiveTvPreferences.channelOrder] == ItemSortBy.DATE_PLAYED.serialName

		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getLiveTvChannels(
					addCurrentProgram = true,
					enableFavoriteSorting = liveTvPreferences[LiveTvPreferences.favsAtTop],
					sortBy = if (sortDatePlayed) setOf(ItemSortBy.DATE_PLAYED) else setOf(ItemSortBy.SORT_NAME),
					sortOrder = if (sortDatePlayed) SortOrder.DESCENDING else SortOrder.ASCENDING,
				).content.items
			}
		}.fold(
			onSuccess = { channels -> callback(channels) },
			onFailure = { callback(null) },
		)
	}
}

fun getPrograms(
	fragment: Fragment,
	channelIds: Array<UUID>,
	startTime: LocalDateTime,
	endTime: LocalDateTime,
	callback: (programs: Collection<BaseItemDto>?) -> Unit,
) {
	val api by fragment.inject<ApiClient>()

	fragment.lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getLiveTvPrograms(
					channelIds = channelIds.toList(),
					enableImages = false,
					sortBy = setOf(ItemSortBy.START_DATE),
					maxStartDate = endTime,
					minEndDate = startTime,
				).content.items
			}
		}.fold(
			onSuccess = { programs -> callback(programs) },
			onFailure = { callback(null) },
		)
	}
}

fun getScheduleRows(
	fragment: Fragment,
	seriesTimerId: String?,
	callback: (timers: Map<LocalDate, List<BaseItemDto>>?) -> Unit,
) {
	val api by fragment.inject<ApiClient>()

	fragment.lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getTimers(
					seriesTimerId = seriesTimerId,
				).content.items
			}
		}.fold(
			onSuccess = { timers ->
				val groupedTimers = timers
					.filterNot { it.startDate == null }
					.map { timer ->
						timer.programInfo ?: BaseItemDto(
							id = requireNotNull(timer.id?.toUUIDOrNull()),
							channelName = timer.channelName,
							name = timer.name.orEmpty(),
							type = BaseItemKind.PROGRAM,
							mediaType = MediaType.UNKNOWN,
							timerId = timer.id,
							seriesTimerId = timer.seriesTimerId,
							startDate = timer.startDate,
							endDate = timer.endDate,
						)
					}
					.map { it.copy(locationType = LocationType.VIRTUAL) }
					.groupBy { it.startDate!!.toLocalDate() }

				callback(groupedTimers)
			},
			onFailure = {
				callback(null)
			},
		)
	}
}
