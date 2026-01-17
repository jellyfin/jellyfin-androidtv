package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import org.jellyfin.androidtv.R
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
