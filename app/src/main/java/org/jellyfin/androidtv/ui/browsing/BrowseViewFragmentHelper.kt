package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDtoQueryResult
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.TimerInfoDto
import org.jellyfin.sdk.model.api.TimerInfoDtoQueryResult
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.inject

fun EnhancedBrowseFragment.getLiveTvRecordingsAndTimers(
	callback: (recordings: BaseItemDtoQueryResult, timers: TimerInfoDtoQueryResult) -> Unit,
	errorCallback: (exception: Throwable) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			val recordings = async(Dispatchers.IO) {
				api.liveTvApi.getRecordings(
					fields = ItemRepository.itemFields,
					enableImages = true,
					limit = 40,
				).content
			}

			val timers = async(Dispatchers.IO) {
				api.liveTvApi.getTimers().content
			}

			recordings.await() to timers.await()
		}.fold(
			onSuccess = { (recordings, timers) -> callback(recordings, timers) },
			onFailure = { exception -> errorCallback(exception) }
		)
	}
}

fun EnhancedBrowseFragment.getLiveTvTimers(
	callback: (timers: TimerInfoDtoQueryResult) -> Unit,
	errorCallback: (exception: Throwable) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getTimers().content
			}
		}.fold(
			onSuccess = { timers -> callback(timers) },
			onFailure = { exception -> errorCallback(exception) }
		)
	}
}

fun getTimerProgramInfo(timer: TimerInfoDto): BaseItemDto {
	val programInfo = timer.programInfo ?: BaseItemDto(
		id = requireNotNull(timer.id?.toUUIDOrNull()),
		channelName = timer.channelName,
		name = timer.name.orEmpty(),
		type = BaseItemKind.PROGRAM,
		timerId = timer.id,
		seriesTimerId = timer.seriesTimerId,
		startDate = timer.startDate,
		endDate = timer.endDate,
		mediaType = MediaType.UNKNOWN,
	)
	return programInfo.copy(
		locationType = LocationType.VIRTUAL,
	)
}
