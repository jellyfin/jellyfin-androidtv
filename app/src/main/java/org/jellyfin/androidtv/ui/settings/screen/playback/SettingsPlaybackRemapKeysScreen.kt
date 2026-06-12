package org.jellyfin.androidtv.ui.settings.screen.playback

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackRemapKeysScreen() {
	val userPreferences = koinInject<UserPreferences>()

	var subtitleKey by rememberPreference(userPreferences, UserPreferences.subtitleKey)
	var audioKey by rememberPreference(userPreferences, UserPreferences.audioKey)

	var remappingTarget by remember { mutableStateOf<RemapTarget?>(null) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_button_remapping)) },
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_subtitle_track_button)) },
				captionContent = { Text(getKeyName(subtitleKey)) },
				onClick = { remappingTarget = RemapTarget.SUBTITLE },
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_audio_track_button)) },
				captionContent = { Text(getKeyName(audioKey)) },
				onClick = { remappingTarget = RemapTarget.AUDIO },
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_Reset)) },
				onClick = {
					subtitleKey = 175
					audioKey = 0
				}
			)
		}
	}

	if (remappingTarget != null) {
		KeyCaptureDialog(
			onKeyCaptured = { keyCode ->
				when (remappingTarget) {
					RemapTarget.SUBTITLE -> subtitleKey = keyCode
					RemapTarget.AUDIO -> audioKey = keyCode
					null -> Unit
				}
				remappingTarget = null
			},
			onDismissRequest = { remappingTarget = null }
		)
	}
}

private enum class RemapTarget {
	SUBTITLE,
	AUDIO,
}

@Composable
private fun getKeyName(keyCode: Int): String {
	return if (keyCode == 0) {
		stringResource(R.string.default_value)
	} else {
		KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
	}
}

@Composable
private fun KeyCaptureDialog(
	onKeyCaptured: (Int) -> Unit,
	onDismissRequest: () -> Unit,
) {
	val focusRequester = remember { FocusRequester() }

	Dialog(onDismissRequest = onDismissRequest) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(JellyfinTheme.colorScheme.scrim)
				.onKeyEvent { keyEvent ->
					if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
						val keyCode = keyEvent.nativeKeyEvent.keyCode
						if (keyCode != KeyEvent.KEYCODE_BACK) {
							onKeyCaptured(keyCode)
							return@onKeyEvent true
						}
					}
					false
				}
				.focusRequester(focusRequester)
				.focusable(),
			contentAlignment = Alignment.Center
		) {
			Column(
				modifier = Modifier
					.background(JellyfinTheme.colorScheme.surface, JellyfinTheme.shapes.medium)
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = stringResource(R.string.pref_button_remapping_description),
					style = JellyfinTheme.typography.default
				)
			}
		}
	}

	LaunchedEffect(Unit) {
		focusRequester.requestFocus()
	}
}
