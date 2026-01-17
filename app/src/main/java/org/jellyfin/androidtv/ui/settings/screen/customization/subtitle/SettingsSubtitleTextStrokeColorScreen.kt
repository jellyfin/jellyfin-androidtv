package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable.SubtitleColorPresetsControl
import org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable.SubtitleStylePreview
import org.jellyfin.androidtv.ui.settings.util.ListColorChannelRangeControl
import org.koin.compose.koinInject

@Composable
fun SettingsSubtitleTextStrokeColorScreen() {
	val userPreferences = koinInject<UserPreferences>()

	var subtitleTextStrokeColor by rememberPreference(userPreferences, UserPreferences.subtitleTextStrokeColor)
	val colorValue = Color(subtitleTextStrokeColor).convert(ColorSpaces.Srgb)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_subtitles).uppercase()) },
				headingContent = { Text(stringResource(R.string.lbl_subtitle_text_stroke_color)) },
			)
		}

		item {
			SubtitleStylePreview(
				userPreferences = userPreferences,
				subtitleTextStrokeColor = subtitleTextStrokeColor,
			)
		}

		// Presets
		item { ListSection(headingContent = { Text(stringResource(R.string.color_presets)) }) }

		item {
			SubtitleColorPresetsControl(
				presets = SubtitleTextStrokeColorPresets,
				value = colorValue,
				onValueChange = { subtitleTextStrokeColor = it.toArgb().toLong() }
			)
		}

		// Color channels

		item { ListSection(headingContent = { Text(stringResource(R.string.color_custom)) }) }

		item {
			ListColorChannelRangeControl(
				headingContent = { Text(stringResource(R.string.color_red)) },
				channel = Color.Red,
				value = colorValue,
				onValueChange = { subtitleTextStrokeColor = it.toArgb().toLong() }
			)
		}

		item {
			ListColorChannelRangeControl(
				headingContent = { Text(stringResource(R.string.color_green)) },
				channel = Color.Green,
				value = colorValue,
				onValueChange = { subtitleTextStrokeColor = it.toArgb().toLong() }
			)
		}

		item {
			ListColorChannelRangeControl(
				headingContent = { Text(stringResource(R.string.color_blue)) },
				channel = Color.Blue,
				value = colorValue,
				onValueChange = { subtitleTextStrokeColor = it.toArgb().toLong() }
			)
		}

		item {
			ListColorChannelRangeControl(
				headingContent = { Text(stringResource(R.string.color_alpha)) },
				channel = Color.Transparent,
				value = colorValue,
				onValueChange = { subtitleTextStrokeColor = it.toArgb().toLong() }
			)
		}
	}
}
