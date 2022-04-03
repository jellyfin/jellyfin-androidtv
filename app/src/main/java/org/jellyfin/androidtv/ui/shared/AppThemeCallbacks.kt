package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.os.Bundle
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import timber.log.Timber

class AppThemeCallbacks(
	private val userPreferences: UserPreferences
) : AbstractActivityLifecycleCallbacks() {
	private var lastTheme: AppTheme? = null
	private var lastPreferencesTheme: AppTheme? = null

	override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
		userPreferences[UserPreferences.appTheme].let {
			Timber.i("Applying theme: %s", it)
			activity.setTheme(ThemeManager.getTheme(activity, it))
			when (activity) {
				is PreferencesActivity -> lastPreferencesTheme = it
				else -> lastTheme = it
			}
		}
	}

	override fun onActivityPreResumed(activity: Activity) {
		val lastThemeForActivity = if (activity is PreferencesActivity) lastPreferencesTheme else lastTheme
		userPreferences[UserPreferences.appTheme].let {
			if (lastThemeForActivity != null && lastThemeForActivity != it) {
				Timber.i("Recreating activity to apply new theme: %s -> %s", lastThemeForActivity, it)
				activity.recreate()
			}
		}
	}
}
