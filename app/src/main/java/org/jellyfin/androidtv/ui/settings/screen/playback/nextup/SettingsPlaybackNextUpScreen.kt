package org.jellyfin.androidtv.ui.settings.screen.playback.nextup

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NEXTUP_TIMER_DISABLED
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.design.Tokens
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun SettingsPlaybackNextUpScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_next_up)) },
			)
		}

		item {
			var mediaQueuingEnabled by rememberPreference(userPreferences, UserPreferences.mediaQueuingEnabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_media_queueing)) },
				trailingContent = { Checkbox(checked = mediaQueuingEnabled) },
				captionContent = { Text(stringResource(R.string.pref_media_queueing_description)) },
				onClick = { mediaQueuingEnabled = !mediaQueuingEnabled }
			)
		}

		item {
			var nextUpBehavior by rememberPreference(userPreferences, UserPreferences.nextUpBehavior)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_next_up_behavior_title)) },
				captionContent = { Text(stringResource(nextUpBehavior.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_NEXT_UP_BEHAVIOR) }
			)
		}

		item {
			var nextUpTimeout by rememberPreference(userPreferences, UserPreferences.nextUpTimeout)
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.pref_next_up_timeout_title)) },
				captionContent = { Text(stringResource(R.string.pref_next_up_timeout_summary)) },
				interactionSource = interactionSource,
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
				) {
					RangeControl(
						modifier = Modifier
							.height(4.dp)
							.weight(1f),
						interactionSource = interactionSource,
						// 0 - 30 seconds with 1 second increment
						min = 0f,
						max = 30_000f,
						stepForward = 1_000f,
						value = nextUpTimeout.toFloat(),
						onValueChange = { nextUpTimeout = it.roundToInt() }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						if (nextUpTimeout == NEXTUP_TIMER_DISABLED) Text(stringResource(R.string.pref_next_up_timeout_disabled))
						else Text("${nextUpTimeout / 1000}s")
					}
				}
			}
		}
	}
}
