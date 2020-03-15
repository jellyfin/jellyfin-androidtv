package org.jellyfin.androidtv.presentation

import android.app.Activity
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preferences.enums.AppTheme
import org.jellyfin.androidtv.preferences.ui.PreferencesActivity

class ThemeManager {
	public companion object {
		fun getTheme(activity: Activity, appTheme: AppTheme): Int {
			// Settings fragment use a special theme with a transparent background
			if (activity is PreferencesActivity) {
				return R.style.Theme_Jellyfin_Preferences
			}

			if (HolidayManager.isAprilFools()) {
				return R.style.Theme_Jellyfin_HotDogStand;
			}

			return when (appTheme) {
				AppTheme.DARK -> R.style.Theme_Jellyfin
				AppTheme.EMERALD -> R.style.Theme_Jellyfin_Legacy
				AppTheme.HOTDOG -> R.style.Theme_Jellyfin_HotDogStand
				else -> R.style.Theme_Jellyfin
			}
		}
	}
}
