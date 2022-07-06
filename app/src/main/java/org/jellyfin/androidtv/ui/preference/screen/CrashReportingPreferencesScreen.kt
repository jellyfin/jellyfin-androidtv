package org.jellyfin.androidtv.ui.preference.screen

import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.TelemetryPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject

class CrashReportingPreferencesScreen : OptionsFragment() {
	private val telemetryPreferences: TelemetryPreferences by inject()

	override val screen by optionsScreen {
		setTitle(R.string.pref_telemetry_category)

		category {
			checkbox {
				setTitle(R.string.pref_crash_reports)
				setContent(R.string.pref_crash_reports_enabled, R.string.pref_crash_reports_disabled)
				bind(telemetryPreferences, TelemetryPreferences.crashReportEnabled)
			}

			checkbox {
				setTitle(R.string.pref_crash_report_logs)
				setContent(R.string.pref_crash_report_logs_enabled, R.string.pref_crash_report_logs_disabled)
				bind(telemetryPreferences, TelemetryPreferences.crashReportIncludeLogs)
				depends { telemetryPreferences[TelemetryPreferences.crashReportEnabled] }
			}
		}
	}
}
