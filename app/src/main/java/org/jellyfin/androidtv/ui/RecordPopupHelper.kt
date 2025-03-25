package org.jellyfin.androidtv.ui

import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.api.TimerInfoDto
import org.koin.android.ext.android.inject
import java.util.UUID

fun SeriesTimerInfoDto.copyWithPrePaddingSeconds(
	prePaddingSeconds: Int,
) = copy(
	prePaddingSeconds = prePaddingSeconds,
)

fun SeriesTimerInfoDto.copyWithPostPaddingSeconds(
	postPaddingSeconds: Int,
) = copy(
	postPaddingSeconds = postPaddingSeconds,
)

fun SeriesTimerInfoDto.copyWithFilters(
	recordNewOnly: Boolean,
	recordAnyChannel: Boolean,
	recordAnyTime: Boolean,
) = copy(
	recordNewOnly = recordNewOnly,
	recordAnyChannel = recordAnyChannel,
	recordAnyTime = recordAnyTime,
)

fun RecordPopup.updateSeriesTimer(
	seriesTimer: SeriesTimerInfoDto,
	callback: () -> Unit,
) {
	val api by mContext.getActivity()!!.inject<ApiClient>()

	lifecycle.coroutineScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				val id = seriesTimer.id
				if (id == null) api.liveTvApi.createSeriesTimer(seriesTimer)
				else api.liveTvApi.updateSeriesTimer(id, seriesTimer)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun RecordPopup.updateTimer(
	timer: TimerInfoDto,
	callback: () -> Unit,
) {
	val api by mContext.getActivity()!!.inject<ApiClient>()

	lifecycle.coroutineScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				val id = timer.id
				if (id == null) api.liveTvApi.createTimer(timer)
				else api.liveTvApi.updateTimer(id, timer)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun createProgramTimerInfo(
	programId: UUID,
	options: SeriesTimerInfoDto,
) = TimerInfoDto(
	programId = programId.toString(),
	prePaddingSeconds = options.prePaddingSeconds,
	postPaddingSeconds = options.postPaddingSeconds,
	isPrePaddingRequired = options.isPrePaddingRequired,
	isPostPaddingRequired = options.isPostPaddingRequired,
)

fun RecordPopup.getLiveTvProgram(
	id: UUID,
	callback: (program: BaseItemDto) -> Unit,
) {
	val api by mContext.getActivity()!!.inject<ApiClient>()

	lifecycle.coroutineScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getProgram(id.toString()).content
			}
		}.onSuccess { program ->
			callback(program)
		}
	}
}
