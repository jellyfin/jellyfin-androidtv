package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.app.Application
import android.os.Bundle
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class AppThemeCallbacks : Application.ActivityLifecycleCallbacks, KoinComponent {
	private var lastTheme: AppTheme? = null
	private var lastPreferencesTheme: AppTheme? = null
	private val userPreferences: UserPreferences by inject()

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
		userPreferences[UserPreferences.appTheme].let {
			Timber.i("Applying theme: %s", it)
			activity.setTheme(ThemeManager.getTheme(activity, it))
			when (activity) {
				is PreferencesActivity -> lastPreferencesTheme = it
				else -> lastTheme = it
			}
		}
	}

	override fun onActivityResumed(activity: Activity) {
		val lastThemeForActivity = if (activity is PreferencesActivity) lastPreferencesTheme else lastTheme
		userPreferences[UserPreferences.appTheme].let {
			if (lastThemeForActivity != null && lastThemeForActivity != it) {
				Timber.i("Recreating activity to apply new theme: %s -> %s", lastThemeForActivity, it)
				activity.recreate()
			}
		}
	}
}
