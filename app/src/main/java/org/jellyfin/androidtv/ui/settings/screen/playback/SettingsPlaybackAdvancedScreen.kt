package org.jellyfin.androidtv.ui.settings.screen.playback

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.getQualityProfiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.form.RangeControl
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListControl
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsAsyncActionListButton
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.util.profile.createDeviceProfileReport
import org.jellyfin.design.Tokens
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.clientLogApi
import org.jellyfin.sdk.model.ServerVersion
import org.koin.compose.koinInject
import java.text.DecimalFormat
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun SettingsPlaybackAdvancedScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	val userSettingPreferences = koinInject<UserSettingPreferences>()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_playback_advanced)) },
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_customization)) }) }

		item {
			var resumeSubtractDuration by rememberPreference(userPreferences, UserPreferences.resumeSubtractDuration)
			val options = getResumeSubtractDurationOptions()

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_resume_preroll)) },
				captionContent = { Text(options[resumeSubtractDuration].orEmpty()) },
				onClick = { router.push(Routes.PLAYBACK_RESUME_SUBTRACT_DURATION) }
			)
		}

		item {
			var skipForwardLength by rememberPreference(userSettingPreferences, UserSettingPreferences.skipForwardLength)
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.skip_forward_length)) },
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
						// 5 - 30 seconds with 5 second increment
						min = 5_000f,
						max = 30_000f,
						stepForward = 5_000f,
						value = skipForwardLength.toFloat(),
						onValueChange = { skipForwardLength = it.roundToInt() }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 32.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						Text("${skipForwardLength / 1000}s")
					}
				}
			}
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_video)) }) }

		item {
			var maxBitrate by rememberPreference(userPreferences, UserPreferences.maxBitrate)
			val options = getQualityProfiles(context)

			ListButton(
				headingContent = { Text(stringResource(R.string.pref_max_bitrate_title)) },
				captionContent = { Text(options[maxBitrate].orEmpty()) },
				onClick = { router.push(Routes.PLAYBACK_MAX_BITRATE) }
			)
		}

		item {
			var refreshRateSwitchingBehavior by rememberPreference(userPreferences, UserPreferences.refreshRateSwitchingBehavior)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_refresh_switching)) },
				captionContent = { Text(stringResource(refreshRateSwitchingBehavior.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_REFRESH_RATE_SWITCHING_BEHAVIOR) }
			)
		}

		item {
			var videoStartDelay by rememberPreference(userPreferences, UserPreferences.videoStartDelay)
			val interactionSource = remember { MutableInteractionSource() }

			ListControl(
				headingContent = { Text(stringResource(R.string.video_start_delay)) },
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
						// 0 - 5 seconds with 0.25 second increment
						min = 0_000f,
						max = 5_000f,
						stepForward = 250f,
						value = videoStartDelay.toFloat(),
						onValueChange = { videoStartDelay = it.roundToLong() }
					)

					Spacer(Modifier.width(Tokens.Space.spaceSm))

					Box(
						modifier = Modifier.sizeIn(minWidth = 48.dp),
						contentAlignment = Alignment.CenterEnd
					) {
						val formatter = remember { DecimalFormat("0.##") }
						Text("${formatter.format(videoStartDelay / 1000f)}s")
					}
				}
			}
		}

		item {
			var playerZoomMode by rememberPreference(userPreferences, UserPreferences.playerZoomMode)

			ListButton(
				headingContent = { Text(stringResource(R.string.default_video_zoom)) },
				captionContent = { Text(stringResource(playerZoomMode.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_ZOOM_MODE) }
			)
		}

		item {
			var pgsDirectPlay by rememberPreference(userPreferences, UserPreferences.pgsDirectPlay)

			ListButton(
				headingContent = { Text(stringResource(R.string.preference_enable_pgs)) },
				trailingContent = { Checkbox(checked = pgsDirectPlay) },
				onClick = { pgsDirectPlay = !pgsDirectPlay }
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_live_tv_cat)) }) }

		item {
			var liveTvDirectPlayEnabled by rememberPreference(userPreferences, UserPreferences.liveTvDirectPlayEnabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_direct_stream_live)) },
				trailingContent = { Checkbox(checked = liveTvDirectPlayEnabled) },
				onClick = { liveTvDirectPlayEnabled = !liveTvDirectPlayEnabled }
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_audio)) }) }

		item {
			var audioBehaviour by rememberPreference(userPreferences, UserPreferences.audioBehaviour)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_audio_output)) },
				captionContent = { Text(stringResource(audioBehaviour.nameRes)) },
				onClick = { router.push(Routes.PLAYBACK_AUDIO_BEHAVIOR) }
			)
		}

		item {
			var audioNightMode by rememberPreference(userPreferences, UserPreferences.audioNightMode)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_direct_stream_live)) },
				trailingContent = { Checkbox(checked = audioNightMode) },
				onClick = { audioNightMode = !audioNightMode }
			)
		}

		item {
			var ac3Enabled by rememberPreference(userPreferences, UserPreferences.ac3Enabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_bitstream_ac3)) },
				trailingContent = { Checkbox(checked = ac3Enabled) },
				onClick = { ac3Enabled = !ac3Enabled }
			)
		}

		item { ListSection(headingContent = { Text(stringResource(R.string.pref_troubleshooting)) }) }

		item {
			val api = koinInject<ApiClient>()
			val serverVersion = koinInject<ServerVersion>()

			SettingsAsyncActionListButton(
				headingContent = { Text(stringResource(R.string.pref_report_device_profile_title)) },
				captionContent = { Text(stringResource(R.string.pref_report_device_profile_summary)) },
				action = {
					val report = createDeviceProfileReport(context, userPreferences, serverVersion)
					val response by api.clientLogApi.logFile(report)
					response
				},
				onSuccess = { result ->
					Toast.makeText(
						context,
						@SuppressLint("LocalContextGetResourceValueCall")
						context.getString(R.string.pref_report_device_profile_success, result.fileName),
						Toast.LENGTH_LONG
					).show()
				},
				onFailure = {
					Toast.makeText(context, R.string.pref_report_device_profile_failure, Toast.LENGTH_LONG).show()
				},
			)
		}
	}
}
