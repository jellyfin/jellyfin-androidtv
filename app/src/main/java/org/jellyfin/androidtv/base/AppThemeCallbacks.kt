package org.jellyfin.androidtv.base

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.preferences.enums.AppTheme
import org.jellyfin.androidtv.preferences.ui.PreferencesActivity
import org.jellyfin.androidtv.presentation.ThemeManager

private const val LOG_TAG = "AppThemeCallbacks"

class AppThemeCallbacks : Application.ActivityLifecycleCallbacks {
	private var lastTheme: AppTheme? = null
	private var lastPreferencesTheme: AppTheme? = null

	override fun onActivityPaused(activity: Activity) {
	}

	override fun onActivityStarted(activity: Activity) {
	}

	override fun onActivityDestroyed(activity: Activity) {
	}

	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
	}

	override fun onActivityStopped(activity: Activity) {
	}

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		TvApp.getApplication().userPreferences.appTheme.let {
			Log.i(LOG_TAG, "Applying theme: $it")
			activity.setTheme(ThemeManager.getTheme(activity, it))
			when (activity) {
				is PreferencesActivity -> lastPreferencesTheme = it
				else -> lastTheme = it
			}
		}
	}

	override fun onActivityResumed(activity: Activity) {
		val lastThemeForActivity = if (activity is PreferencesActivity) lastPreferencesTheme else lastTheme
		TvApp.getApplication().userPreferences.appTheme.let {
			if (lastThemeForActivity != null && lastThemeForActivity != it) {
				Log.i(LOG_TAG, "Recreating activity to apply new theme: $lastThemeForActivity -> $it")
				activity.recreate()
			}
		}
	}
}
