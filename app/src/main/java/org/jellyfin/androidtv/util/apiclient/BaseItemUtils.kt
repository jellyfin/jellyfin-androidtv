@file:JvmName("BaseItemUtils")

package org.jellyfin.androidtv.util.apiclient

import android.content.Context
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto

fun SeriesTimerInfoDto.getSeriesOverview(context: Context) = buildString {
	if (recordNewOnly == true) appendLine(context.getString(R.string.lbl_record_only_new))
	else appendLine(context.getString(R.string.lbl_record_all))

	if (recordAnyChannel == true) appendLine(context.getString(R.string.lbl_on_any_channel))
	else appendLine(context.getString(R.string.lbl_on_channel, channelName))

	append(dayPattern)
	if (recordAnyTime == true) append(" ", context.getString(R.string.lbl_at_any_time))

	appendLine()

	when {
		prePaddingSeconds!! > 0 -> when {
			postPaddingSeconds!! > 0 -> append(
				context.getString(
					R.string.lbl_starting_early_ending_after,
					TimeUtils.formatSeconds(context, prePaddingSeconds!!),
					TimeUtils.formatSeconds(context, postPaddingSeconds!!)
				)
			)
			else -> append(
				context.getString(
					R.string.lbl_starting_early_ending_on_schedule,
					TimeUtils.formatSeconds(context, prePaddingSeconds!!)
				)
			)
		}
		else -> when {
			postPaddingSeconds!! > 0 -> append(
				context.getString(
					R.string.lbl_starting_on_schedule_ending_after,
					TimeUtils.formatSeconds(context, postPaddingSeconds!!)
				)
			)
			else -> append(context.getString(R.string.lbl_start_end_on_schedule))
		}
	}
}
