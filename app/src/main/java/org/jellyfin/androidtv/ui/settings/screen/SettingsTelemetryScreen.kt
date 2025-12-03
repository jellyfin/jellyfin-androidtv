package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.koin.compose.koinInject

@Composable
fun SettingsTelemetryScreen() {
	val telemetryPreferences = koinInject<TelemetryPreferences>()

	Column(
		modifier = Modifier
			.verticalScroll(rememberScrollState())
			.padding(6.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		ListSection(
			modifier = Modifier,
			overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
			headingContent = { Text(stringResource(R.string.pref_telemetry_category)) },
			captionContent = { Text(stringResource(R.string.pref_telemetry_description)) },
		)

		var crashReportEnabled by rememberPreference(telemetryPreferences, TelemetryPreferences.crashReportEnabled)
		ListButton(
			headingContent = { Text(stringResource(R.string.pref_crash_reports)) },
			trailingContent = {
				RadioButton(
					checked = crashReportEnabled,
				)
			},
			captionContent = {
				if (crashReportEnabled) {
					Text(stringResource(R.string.pref_crash_reports_enabled))
				} else {
					Text(stringResource(R.string.pref_crash_reports_disabled))
				}
			},
			onClick = {
				crashReportEnabled = !crashReportEnabled
			}
		)

		var crashReportIncludeLogs by rememberPreference(telemetryPreferences, TelemetryPreferences.crashReportIncludeLogs)
		ListButton(
			headingContent = { Text(stringResource(R.string.pref_crash_report_logs)) },
			trailingContent = {
				RadioButton(
					checked = crashReportIncludeLogs,
				)
			},
			captionContent = {
				if (crashReportIncludeLogs) {
					Text(stringResource(R.string.pref_crash_report_logs_enabled))
				} else {
					Text(stringResource(R.string.pref_crash_report_logs_disabled))
				}
			},
			onClick = {
				crashReportIncludeLogs = !crashReportIncludeLogs
			}
		)
	}
}
