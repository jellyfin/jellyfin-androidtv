package org.jellyfin.androidtv.ui.settings.screen.playback.mediasegment

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackMediaSegmentsScreen() {
	val router = LocalRouter.current
	val mediaSegmentRepository = koinInject<MediaSegmentRepository>()
	val userPreferences = koinInject<UserPreferences>()

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
				},
				modifier = Modifier.focusKey("media_segment_type_$segmentType")
			)
		}

		item {
			val autoHideDuration by rememberPreference(userPreferences, UserPreferences.mediaSegmentAutoHideDuration)
			val options = getMediaSegmentAutoHideDurationOptions()

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_skip_button_duration)) },
				captionContent = { Text(options[autoHideDuration].orEmpty()) },
				onClick = { router.push(Routes.PLAYBACK_MEDIA_SEGMENT_AUTO_HIDE_DURATION) },
				modifier = Modifier.focusKey(Routes.PLAYBACK_MEDIA_SEGMENT_AUTO_HIDE_DURATION)
			)
		}
	}
}
