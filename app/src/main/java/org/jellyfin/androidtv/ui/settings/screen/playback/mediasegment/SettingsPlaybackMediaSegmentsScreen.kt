package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackMediaSegmentsScreen() {
	val router = LocalRouter.current
	val mediaSegmentRepository = koinInject<MediaSegmentRepository>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_media_segments)) },
			)
		}

		items(MediaSegmentRepository.SupportedTypes) { segmentType ->
			val action = mediaSegmentRepository.getDefaultSegmentTypeAction(segmentType)

			ListButton(
				headingContent = { Text(stringResource(segmentType.nameRes)) },
				captionContent = { Text(stringResource(action.nameRes)) },
				onClick = {
					router.push(
						route = Routes.PLAYBACK_MEDIA_SEGMENT,
						parameters = mapOf(
							"segmentType" to segmentType.toString(),
						),
					)
				}
			)
		}
	}
}
