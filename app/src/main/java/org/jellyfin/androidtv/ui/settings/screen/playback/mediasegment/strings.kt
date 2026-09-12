package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.sdk.model.api.MediaSegmentType

val MediaSegmentType.nameRes
	get() = when (this) {
		MediaSegmentType.UNKNOWN -> R.string.segment_type_unknown
		MediaSegmentType.COMMERCIAL -> R.string.segment_type_commercial
		MediaSegmentType.PREVIEW -> R.string.segment_type_preview
		MediaSegmentType.RECAP -> R.string.segment_type_recap
		MediaSegmentType.OUTRO -> R.string.segment_type_outro
		MediaSegmentType.INTRO -> R.string.segment_type_intro
	}

@Composable
@Stable
fun getMediaSegmentAutoHideDurationOptions(): Map<Int, String> {
	val context = LocalContext.current
	return listOf(
		3,
		5,
		8,
		10,
		15,
		20,
		30,
		0,
	).associateWith { seconds ->
		if (seconds == 0) stringResource(R.string.pref_skip_button_until_segment_ends)
		else TimeUtils.formatSeconds(context, seconds)
	}
}
