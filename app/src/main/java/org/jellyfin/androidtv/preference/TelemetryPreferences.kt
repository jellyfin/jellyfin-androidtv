package org.jellyfin.androidtv.preference

import android.content.Context
import org.acra.ACRA
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference

class TelemetryPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
) {
	companion object {
		const val SHARED_PREFERENCES_NAME = "telemetry"

		var crashReportEnabled = booleanPreference(ACRA.PREF_ENABLE_ACRA, true)
		var crashReportIncludeLogs = booleanPreference(ACRA.PREF_ENABLE_SYSTEM_LOGS, true)

		var crashReportToken = stringPreference("server_token", "")
		var crashReportUrl = stringPreference("server_url", "")
	}
}
