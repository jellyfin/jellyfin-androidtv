package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.koin.compose.koinInject
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jellyfin.androidtv.constant.Codec
import org.jellyfin.androidtv.util.profile.supportedAudioCodecs_default
import org.jellyfin.androidtv.ui.base.button.Button
import kotlin.collections.filterNot


@Composable
fun SettingsPlaybackPreferredAudioCodecScreen()
{

	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var userdefinedaudiocodecs by rememberPreference(userPreferences, UserPreferences.userdefinedaudiocodecs)

	var forceredraw by remember { mutableStateOf(0) }
	var isOpen by remember { mutableStateOf(true) }
	if (isOpen)
	{

		var index : Int

		var stemp: String
		var defaultcodecs: Array<String>
		var currentcodecs: Array<String>
		var isAC3Enabled: Boolean
		var isAACEnabled: Boolean

		isAC3Enabled = userPreferences[UserPreferences.ac3Enabled]
		isAACEnabled = userPreferences[UserPreferences.aacEnabled]

		defaultcodecs = supportedAudioCodecs_default
		if (!isAC3Enabled)
		{
			defaultcodecs.filterNot { it == Codec.Audio.EAC3 || it == Codec.Audio.AC3 }.toTypedArray()
		}
		if (!isAACEnabled)
		{
			defaultcodecs.filterNot { it == Codec.Audio.AAC || it == Codec.Audio.AAC_LATM }.toTypedArray()
		}

		currentcodecs = userdefinedaudiocodecs.split(",").toTypedArray()
		if (!isAC3Enabled)
		{
			currentcodecs = currentcodecs.filterNot { it == Codec.Audio.EAC3 || it == Codec.Audio.AC3 }.toTypedArray()
		}
		if (!isAACEnabled)
		{
			currentcodecs = currentcodecs.filterNot { it == Codec.Audio.AAC || it == Codec.Audio.AAC_LATM  }.toTypedArray()
		}

		val buttonwidth: Dp = 60.dp
		val resetbuttonwidth: Dp = 100.dp
		val buttonheight: Dp = 23.dp
		val headertextsize: TextUnit = 14.sp
		val textsize: TextUnit = 10.sp
		val codecboxwidth = 70.dp
		val codecboxheight = buttonheight
		val codecboxtextsize: TextUnit = 14.sp

		Popup(
			alignment = Alignment.Center,
			onDismissRequest = { isOpen = false },
			properties = PopupProperties(
				focusable = true,
				dismissOnBackPress = true
			)
		) {

			Box(
				modifier = Modifier
					.background(Color.Black)
					.padding(1.dp)
					.requiredSize(width = 800.dp, height = 550.dp)
			) {

				Column(
					modifier = Modifier
						.wrapContentSize(Alignment.Center)
						.background(color = Color.Black)
						.padding(1.dp),
					verticalArrangement = Arrangement.spacedBy(1.dp)

				)
				{
					Row()
					{
						Text(
							text = stringResource(R.string.pref_playback_preferred_audio_codecs_string_1),
							textAlign = TextAlign.Left,
							modifier = Modifier.padding(1.dp),
							style = TextStyle(
								color = Color.White,
								fontSize = headertextsize
							)
						)
					}

					Row()
					{
						Text(
							text = stringResource(R.string.pref_playback_preferred_audio_codecs_string_2),
							textAlign = TextAlign.Left,
							modifier = Modifier.padding(1.dp),
							style = TextStyle(
								color = Color.White,
								fontSize = headertextsize
							)
						)
					}

					/*
					Row()
					{
						Text(
							text = stringResource(R.string.pref_playback_preferred_audio_codecs_string_3),
							textAlign = TextAlign.Left,
							modifier = Modifier.padding(1.dp),
							style = TextStyle(
								color = Color.White,
								fontSize = headertextsize
							)
						)
					}
					*/

					Row(
					)
					{
						Text(
							text = userdefinedaudiocodecs,
							textAlign = TextAlign.Left,
							style = TextStyle(
								color = Color.White,
								fontSize = headertextsize
							)
						)
					}

					Row()
					{
						// This is the reset to defaults button
						Button(
							onClick = {
								currentcodecs = defaultcodecs
								userdefinedaudiocodecs = defaultcodecs.joinToString(",")
								forceredraw++
							},
							modifier = Modifier
								.width(resetbuttonwidth)
								.height(buttonheight)
								.padding(1.dp),
							contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp),
						) {
							Text(
								text = stringResource(R.string.pref_playback_preferred_audio_codecs_reset),
								textAlign = TextAlign.Center,
								modifier = Modifier
									.padding(1.dp)
									.fillMaxWidth(),
								style = TextStyle(
									color = Color.White,
									fontSize = textsize
								)
							)
						}

						// This is the exit button
						/*
						Button(
							onClick = { isOpen = false },
							modifier = Modifier
								.width(resetbuttonwidth)
								.height(buttonheight)
								.padding(1.dp),
							contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
						) {
							Text(
								text = stringResource(R.string.pref_playback_preferred_audio_codecs_exit),
								textAlign = TextAlign.Center,
								modifier = Modifier.padding(1.dp),
								style = TextStyle(
									color = Color.White,
									fontSize = textsize
								)
							)
						}
						*/

					}

					for (i in 0 until defaultcodecs.size) {
						Row(
							horizontalArrangement = Arrangement.spacedBy(2.dp),
							verticalAlignment = Alignment.CenterVertically,
						)
						{

							// this is the label (audio codec) for the row of buttons
							Box(
								contentAlignment = Alignment.CenterEnd,
								modifier = Modifier
									.size(width = codecboxwidth, height = codecboxheight)
									.padding(1.dp)
							) {
								Text(
									text = defaultcodecs[i],
									modifier = Modifier
										.padding(1.dp)
										.fillMaxWidth(),
									style = TextStyle(
										color = Color.White,
										fontSize = codecboxtextsize
									)
								)
							}

							// This is the move left button
							Button(
								onClick = {
									stemp = defaultcodecs[i]
									index = currentcodecs.indexOf(stemp)
									if (index > 0) {
										stemp = currentcodecs[index - 1]
										currentcodecs[index - 1] = currentcodecs[index]
										currentcodecs[index] = stemp
										userdefinedaudiocodecs = currentcodecs.joinToString(",")
										forceredraw++
									}
								},
								modifier = Modifier
									.width(buttonwidth)
									.height(buttonheight)
									.padding(1.dp),
								contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
							) {
								Text(
									text = stringResource(R.string.pref_playback_preferred_audio_codecs_move_left),
									textAlign = TextAlign.Center,
									modifier = Modifier
										.padding(1.dp)
										.fillMaxWidth(),
									style = TextStyle(
										color = Color.White,
										fontSize = textsize
									)
								)
							}

							// This is the move right button
							Button(
								onClick = {
									stemp = defaultcodecs[i]
									index = currentcodecs.indexOf(stemp)
									if (index >= 0 && index < currentcodecs.size - 1) {
										stemp = currentcodecs[index + 1]
										currentcodecs[index + 1] = currentcodecs[index]
										currentcodecs[index] = stemp
										userdefinedaudiocodecs = currentcodecs.joinToString(",")
										forceredraw++
									}
								},
								modifier = Modifier
									.width(buttonwidth)
									.height(buttonheight)
									.padding(1.dp),
								contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
							) {
								Text(
									text = stringResource(R.string.pref_playback_preferred_audio_codecs_move_right),
									textAlign = TextAlign.Center,
									modifier = Modifier
										.padding(1.dp)
										.fillMaxWidth(),
									style = TextStyle(
										color = Color.White,
										fontSize = textsize
									)
								)
							}

							// This is the delete button
							Button(
								onClick = {
									// the rows are based on the defaultcodecs array
									currentcodecs = currentcodecs.filterNot { it == defaultcodecs[i] }.toTypedArray()
									userdefinedaudiocodecs = currentcodecs.joinToString(",")
									forceredraw++
								},
								modifier = Modifier
									.width(buttonwidth)
									.height(buttonheight)
									.padding(1.dp),
								contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
							) {
								Text(
									text = stringResource(R.string.pref_playback_preferred_audio_codecs_delete),
									textAlign = TextAlign.Center,
									modifier = Modifier
										.padding(1.dp)
										.fillMaxWidth(),
									style = TextStyle(
										color = Color.White,
										fontSize = textsize
									)
								)
							}

							// This is the add button
							Button(
								onClick = {
									// only add it if it doesn't already exist
									if (!currentcodecs.contains(defaultcodecs[i])) {
										currentcodecs = currentcodecs + defaultcodecs[i]
										userdefinedaudiocodecs = currentcodecs.joinToString(",")
										forceredraw++
									}
								},
								modifier = Modifier
									.width(buttonwidth)
									.height(buttonheight)
									.padding(1.dp),
								contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
							) {
								Text(
									text = stringResource(R.string.pref_playback_preferred_audio_codecs_add),
									textAlign = TextAlign.Center,
									modifier = Modifier
										.padding(1.dp)
										.fillMaxWidth(),
									style = TextStyle(
										color = Color.White,
										fontSize = textsize
									)
								)
							}

						}
					}

				}
			}

		}
	}
	else
	{
		LaunchedEffect(Unit) {
			router.back()
		}
	}
}










