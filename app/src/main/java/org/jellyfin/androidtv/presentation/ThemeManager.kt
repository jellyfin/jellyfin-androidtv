package org.jellyfin.androidtv.presentation

import android.app.Activity
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.enums.AppTheme
import org.jellyfin.androidtv.preferences.ui.PreferencesActivity

object ThemeManager {
	fun getTheme(activity: Activity, appTheme: AppTheme): Int {
		// Settings fragment use a special theme with a transparent background
		if (activity is PreferencesActivity) {
			return R.style.Theme_Jellyfin_Preferences
		}

		if (HolidayManager.isAprilFools()) {
			return R.style.Theme_Jellyfin_HotDogStand;
		}

		return when (appTheme) {
			AppTheme.Theme_Jellyfin -> R.style.Theme_Jellyfin
			AppTheme.Theme_Jellyfin_Emerald -> R.style.Theme_Jellyfin_Emerald
			AppTheme.Theme_Jellyfin_HotDogStand -> R.style.Theme_Jellyfin_HotDogStand
			else -> R.style.Theme_Jellyfin
		}
	}
}
