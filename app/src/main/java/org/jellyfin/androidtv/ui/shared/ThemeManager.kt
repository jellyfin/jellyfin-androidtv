package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.koin.android.ext.android.get
import java.util.*

object ThemeManager {
	private fun showAprilFools(userPreferences: UserPreferences): Boolean {
		val enableGreetings = userPreferences[UserPreferences.seasonalGreetingsEnabled]
		if (!enableGreetings) return false

		val today = GregorianCalendar()
		return today[Calendar.MONTH] == Calendar.APRIL && today[Calendar.DAY_OF_MONTH] == 1
	}

	fun getTheme(activity: Activity, appTheme: AppTheme): Int {
		// Settings fragment use a special theme with a transparent background
		if (activity is PreferencesActivity) {
			return R.style.Theme_Jellyfin_Preferences
		}

		if (showAprilFools(activity.get())) {
			return R.style.Theme_Jellyfin_HotDogStand;
		}

		return when (appTheme) {
			AppTheme.DARK -> R.style.Theme_Jellyfin
			AppTheme.EMERALD -> R.style.Theme_Jellyfin_Emerald
			AppTheme.MUTED_PURPLE -> R.style.Theme_Jellyfin_MutedPurple
			AppTheme.HOT_DOG_STAND -> R.style.Theme_Jellyfin_HotDogStand
		}
	}
}
