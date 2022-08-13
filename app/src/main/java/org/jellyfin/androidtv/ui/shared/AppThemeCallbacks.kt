package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.os.Build
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) activity.applyThemeOnCreated()
	}

	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) activity.applyThemeOnCreated()
	}

	override fun onActivityPreResumed(activity: Activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) activity.applyThemeOnResume()
	}

	override fun onActivityResumed(activity: Activity) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) activity.applyThemeOnResume()
	}

	private fun Activity.applyThemeOnCreated() {
		userPreferences[UserPreferences.appTheme].let {
			Timber.i("Applying theme: %s", it)
			setTheme(ThemeManager.getTheme(this, it))
			when (this) {
				is PreferencesActivity -> lastPreferencesTheme = it
				else -> lastTheme = it
			}
		}
	}

	private fun Activity.applyThemeOnResume() {
		val lastThemeForActivity = if (this is PreferencesActivity) lastPreferencesTheme else lastTheme
		userPreferences[UserPreferences.appTheme].let {
			if (lastThemeForActivity != null && lastThemeForActivity != it) {
				Timber.i("Recreating activity to apply new theme: %s -> %s", lastThemeForActivity, it)
				recreate()
			}
		}
	}
}
