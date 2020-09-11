package org.jellyfin.androidtv.ui.base

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log

import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.startup.DpadPwActivity
import org.jellyfin.androidtv.ui.startup.SelectServerActivity
import org.jellyfin.androidtv.ui.startup.SelectUserActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity

private const val LOG_TAG = "AuthUserCallbacks"

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
			is DpadPwActivity,
			is SelectServerActivity,
			is SelectUserActivity,
			is StartupActivity -> return
			// All other activities should have a current user
			else -> {
				TvApp.getApplication().apply {
					if (currentUser == null) {
						Log.w(LOG_TAG, "Current user is null, bouncing to StartupActivity")
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
