package org.jellyfin.androidtv.ui.shared

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity
import timber.log.Timber

/**
 * ActivityLifecycleCallback that bounces to the StartupActivity if an Activity is created and
 * the currentUser is null.
 */
class AuthenticatedUserCallbacks : Application.ActivityLifecycleCallbacks {
	override fun onActivityPaused(activity: Activity) {
	}

	override fun onActivityStarted(activity: Activity) {
	}

	override fun onActivityDestroyed(activity: Activity) {
	}

	override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
	}

	override fun onActivityStopped(activity: Activity) {
	}

	override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
		when (activity) {
			// Ignore startup activities
			is StartupActivity -> return
			is PreferencesActivity -> return
			is org.acra.dialog.CrashReportDialog -> return
			// All other activities should have a current user
			else -> {
				TvApp.getApplication().apply {
					if (currentUser == null) {
						Timber.w("Current user is null, bouncing to StartupActivity")
						activity.startActivity(Intent(this, StartupActivity::class.java))
						activity.finish()
					}
				}
			}
		}
	}

	override fun onActivityResumed(activity: Activity) {
	}
}
