package org.jellyfin.androidtv.ui.preference

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.list.TvLazyColumn
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R

@OptIn(ExperimentalTvFoundationApi::class)
@Composable
internal fun MainPreferencesScreen(
	modifier: Modifier = Modifier,
) {
	val focusRequester = remember { FocusRequester() }
	TvLazyColumn(
		modifier = modifier,
		contentPadding = PaddingValues(bottom = 26.dp),
	) {
		stickyHeader {
			PreferencesHeader(
				modifier = Modifier
					.padding(bottom = 26.dp),
				text = stringResource(id = R.string.settings_title),
			)
		}
		item {
			PreferenceItem(
				modifier = Modifier
					.focusRequester(focusRequester),
				iconRes = R.drawable.ic_users,
				title = stringResource(id = R.string.pref_login),
				description = stringResource(id = R.string.pref_login_description),
				onClick = {
					// TODO
				},
			)
		}
		item {
			PreferenceItem(
				iconRes = R.drawable.ic_adjust,
				title = stringResource(id = R.string.pref_customization),
				description = stringResource(id = R.string.pref_customization_description),
				onClick = {
					// TODO
				},
			)
		}
		item {
			PreferenceItem(
				iconRes = R.drawable.ic_next,
				title = stringResource(id = R.string.pref_playback),
				description = stringResource(id = R.string.pref_playback_description),
				onClick = {
					// TODO
				},
			)
		}
		item {
			PreferenceItem(
				iconRes = R.drawable.ic_error,
				title = stringResource(id = R.string.pref_telemetry_category),
				description = stringResource(id = R.string.pref_telemetry_description),
				onClick = {
					// TODO
				},
			)
		}
		item {
			PreferenceItem(
				iconRes = R.drawable.ic_flask,
				title = stringResource(id = R.string.pref_developer_link),
				description = stringResource(id = R.string.pref_developer_link_description),
				onClick = {
					// TODO
				},
			)
		}

		// About
		item {
			PreferenceCategory(
				title = stringResource(id = R.string.pref_about_title),
			)
		}
		item {
			PreferenceItem(
				iconRes = R.drawable.ic_jellyfin,
				title = "Jellyfin app version",
				description = "jellyfin-androidtv ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}",
				onClick = {
					// No action
				},
			)
		}
		item {
			PreferenceItem(
				iconRes = R.drawable.ic_tv,
				title = stringResource(id = R.string.pref_device_model),
				description = "${Build.MANUFACTURER} ${Build.MODEL}",
				onClick = {
					// No action
				},
			)
		}
		item {
			PreferenceItem(
				iconRes = R.drawable.ic_guide,
				title = stringResource(id = R.string.licenses_link),
				description = stringResource(id = R.string.licenses_link_description),
				onClick = {
					// TODO
				},
			)
		}
	}

	LaunchedEffect(Unit) {
		focusRequester.requestFocus()
	}
}

@Preview
@Composable
private fun Preview() {
	MainPreferencesScreen()
}
