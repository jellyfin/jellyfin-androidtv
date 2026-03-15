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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.getQualityProfiles
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Icon
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
			var PreferredAudioCodec by rememberPreference(userPreferences, UserPreferences.preferred_audio_codec)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_preferred_audio_codec)) },
				captionContent = { Text(stringResource(PreferredAudioCodec.nameRes))},
				onClick = { router.push(Routes.PLAYBACK_PREFERRED_AUDIO_CODEC) }
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

		item {
			var eac3Enabled by rememberPreference(userPreferences, UserPreferences.eac3Enabled)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_bitstream_eac3)) },
				trailingContent = { Checkbox(checked = eac3Enabled) },
				onClick = { eac3Enabled = !eac3Enabled }
			)
		}

		item {
			var disable_aac by rememberPreference(userPreferences, UserPreferences.disable_aac)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_aac)) },
				trailingContent = { Checkbox(checked = disable_aac) },
				onClick = { disable_aac = !disable_aac }
			)
		}

		item {
			var disable_aac_latm by rememberPreference(userPreferences, UserPreferences.disable_aac_latm)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_aac_latm)) },
				trailingContent = { Checkbox(checked = disable_aac_latm) },
				onClick = { disable_aac_latm = !disable_aac_latm }
			)
		}

		item {
			var disable_alac by rememberPreference(userPreferences, UserPreferences.disable_alac)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_alac)) },
				trailingContent = { Checkbox(checked = disable_alac) },
				onClick = { disable_alac = !disable_alac }
			)
		}

		item {
			var disable_dca by rememberPreference(userPreferences, UserPreferences.disable_dca)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_dca)) },
				trailingContent = { Checkbox(checked = disable_dca) },
				onClick = { disable_dca = !disable_dca }
			)
		}

		item {
			var disable_dts by rememberPreference(userPreferences, UserPreferences.disable_dts)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_dts)) },
				trailingContent = { Checkbox(checked = disable_dts) },
				onClick = { disable_dts = !disable_dts }
			)
		}

		item {
			var disable_flac by rememberPreference(userPreferences, UserPreferences.disable_flac)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_flac)) },
				trailingContent = { Checkbox(checked = disable_flac) },
				onClick = { disable_flac = !disable_flac }
			)
		}

		item {
			var disable_mlp by rememberPreference(userPreferences, UserPreferences.disable_mlp)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_mlp)) },
				trailingContent = { Checkbox(checked = disable_mlp) },
				onClick = { disable_mlp = !disable_mlp }
			)
		}

		item {
			var disable_mp2 by rememberPreference(userPreferences, UserPreferences.disable_mp2)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_mp2)) },
				trailingContent = { Checkbox(checked = disable_mp2) },
				onClick = { disable_mp2 = !disable_mp2 }
			)
		}

		item {
			var disable_mp3 by rememberPreference(userPreferences, UserPreferences.disable_mp3)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_mp3)) },
				trailingContent = { Checkbox(checked = disable_mp3) },
				onClick = { disable_mp3 = !disable_mp3 }
			)
		}

		item {
			var disable_opus by rememberPreference(userPreferences, UserPreferences.disable_opus)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_opus)) },
				trailingContent = { Checkbox(checked = disable_opus) },
				onClick = { disable_opus = !disable_opus }
			)
		}

		item {
			var disable_pcm_alaw by rememberPreference(userPreferences, UserPreferences.disable_pcm_alaw)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_pcm_alaw)) },
				trailingContent = { Checkbox(checked = disable_pcm_alaw) },
				onClick = { disable_pcm_alaw = !disable_pcm_alaw }
			)
		}

		item {
			var disable_pcm_mulaw by rememberPreference(userPreferences, UserPreferences.disable_pcm_mulaw)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_pcm_mulaw)) },
				trailingContent = { Checkbox(checked = disable_pcm_mulaw) },
				onClick = { disable_pcm_mulaw = !disable_pcm_mulaw }
			)
		}

		item {
			var disable_pcm_s16le by rememberPreference(userPreferences, UserPreferences.disable_pcm_s16le)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_pcm_s16le)) },
				trailingContent = { Checkbox(checked = disable_pcm_s16le) },
				onClick = { disable_pcm_s16le = !disable_pcm_s16le }
			)
		}

		item {
			var disable_pcm_s20le by rememberPreference(userPreferences, UserPreferences.disable_pcm_s20le)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_pcm_s20le)) },
				trailingContent = { Checkbox(checked = disable_pcm_s20le) },
				onClick = { disable_pcm_s20le = !disable_pcm_s20le }
			)
		}

		item {
			var disable_pcm_s24le by rememberPreference(userPreferences, UserPreferences.disable_pcm_s24le)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_pcm_s24le)) },
				trailingContent = { Checkbox(checked = disable_pcm_s24le) },
				onClick = { disable_pcm_s24le = !disable_pcm_s24le }
			)
		}

		item {
			var disable_truehd by rememberPreference(userPreferences, UserPreferences.disable_truehd)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_truehd)) },
				trailingContent = { Checkbox(checked = disable_truehd) },
				onClick = { disable_truehd = !disable_truehd }
			)
		}

		item {
			var disable_vorbis by rememberPreference(userPreferences, UserPreferences.disable_vorbis)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_disable_vorbis)) },
				trailingContent = { Checkbox(checked = disable_vorbis) },
				onClick = { disable_vorbis = !disable_vorbis }
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
