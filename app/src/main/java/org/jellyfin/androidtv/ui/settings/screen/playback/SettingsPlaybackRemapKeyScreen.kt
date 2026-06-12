package org.jellyfin.androidtv.ui.settings.screen.playback

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackRemapKeyScreen(target: String) {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	val focusRequester = remember { FocusRequester() }

	val titleRes = if (target == "subtitle") R.string.pref_subtitle_track_button else R.string.pref_audio_track_button

	Box(
		modifier = Modifier
			.fillMaxSize()
			.onKeyEvent { keyEvent ->
				if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
					val keyCode = keyEvent.nativeKeyEvent.keyCode
					if (keyCode != KeyEvent.KEYCODE_BACK) {
						if (target == "subtitle") {
							userPreferences[UserPreferences.subtitleKey] = keyCode
						} else {
							userPreferences[UserPreferences.audioKey] = keyCode
						}
						router.back()
						return@onKeyEvent true
					}
				}
				false
			}
			.focusRequester(focusRequester)
			.focusable(),
		contentAlignment = Alignment.Center
	) {
		SettingsColumn {
			item {
				ListSection(
					overlineContent = { Text(stringResource(R.string.pref_button_remapping).uppercase()) },
					headingContent = { Text(stringResource(titleRes)) },
					captionContent = { Text(stringResource(R.string.pref_button_remapping_description)) }
				)
			}
		}
	}

	LaunchedEffect(Unit) {
		focusRequester.requestFocus()
	}
}
