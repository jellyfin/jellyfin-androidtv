package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.ui.preference.PreferencesActivity

object ThemeManager {
	fun getTheme(activity: Activity, appTheme: AppTheme): Int {
		// Settings fragment use a special theme with a transparent background
		if (activity is PreferencesActivity) {
			return R.style.Theme_Jellyfin_Preferences
		}

		return when (appTheme) {
			AppTheme.DARK -> R.style.Theme_Jellyfin
			AppTheme.EMERALD -> R.style.Theme_Jellyfin_Emerald
			AppTheme.MUTED_PURPLE -> R.style.Theme_Jellyfin_MutedPurple
		}
	}
}
