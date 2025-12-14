package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentAction
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.sdk.model.api.MediaSegmentType
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackMediaSegmentScreen(
	segmentType: MediaSegmentType
) {
	val router = LocalRouter.current
	val mediaSegmentRepository = koinInject<MediaSegmentRepository>()
	val action = mediaSegmentRepository.getDefaultSegmentTypeAction(segmentType)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_media_segments).uppercase()) },
				headingContent = { Text(stringResource(segmentType.nameRes)) },
			)
		}

		items(MediaSegmentAction.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				trailingContent = { RadioButton(checked = action == entry) },
				onClick = {
					mediaSegmentRepository.setDefaultSegmentTypeAction(segmentType, entry)
					router.back()
				}
			)
		}
	}
}
